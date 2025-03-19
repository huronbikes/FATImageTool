package org.huronbikes.dos.FAT;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.huronbikes.dos.ByteUtils;
import org.huronbikes.dos.Directory.*;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import static org.huronbikes.dos.ByteUtils.parseLong;

public class FAT16 implements FAT {

    public void writeRootDirectory(List<DirectoryItemEntry> directoryEntries) throws IOException {
        try(var channel = openForWrite()) {
            channel.position(rootDirectoryOffset);
            var buffer = ByteBuffer.allocate(rootDirectoryEntries * DirectoryItemEntry.BYTES_PER_DIRECTORY_ENTRY);
            Arrays.fill(buffer.array(), (byte) 0);
            var directoryEntryBuffer = new byte[DirectoryItemEntry.BYTES_PER_DIRECTORY_ENTRY];
            for(var entry : directoryEntries) {
                entry.writeDirectoryEntry(directoryEntryBuffer);
                buffer.put(directoryEntryBuffer, 0, directoryEntryBuffer.length);
            }
            buffer.position(0);
            channel.write(buffer);
        }
    }

    @RequiredArgsConstructor
    @Getter
    private static class ClusterList {
        private final int clusterNumber;
        @Setter
        private ClusterList next;
        @Setter
        private int size = 1;
        public void incrementSize() {
            size++;
        }
    }

    private static final int BYTES_PER_ENTRY = 2;
    private final ByteBuffer data;
    private final int bytesPerFat;
    private final int fatCopies;
    private final long fatOffset;
    @Getter
    private final int bytesPerCluster;
    private final int clusterCount;
    private final AtomicReference<ClusterList> freeSpace = new AtomicReference<>();
    private final File imageFile;
    private final int endOfRecordMarker;
    private final int mediaTypeMarker;
    private final int rootDirectoryEntries;
    private final long rootDirectoryOffset;
    private final long dataOffset;

    public FAT16(
            File imageFile,
            FileChannel imageChannel,
            int bytesPerFat,
            int fatCopies,
            long fatOffset,
            int bytesPerCluster,
            int clusterCount,
            int rootDirectoryEntries,
            long rootDirectoryOffset,
            long dataOffset
    ) throws IOException {
        this.imageFile = imageFile;
        this.bytesPerFat = bytesPerFat;
        this.fatCopies = fatCopies;
        this.fatOffset = fatOffset;
        this.bytesPerCluster = bytesPerCluster;
        this.clusterCount = clusterCount;
        this.rootDirectoryEntries = rootDirectoryEntries;
        this.rootDirectoryOffset = rootDirectoryOffset;
        this.dataOffset = dataOffset;
        imageChannel.position(fatOffset);
        data = ByteBuffer.allocate(bytesPerFat);
        imageChannel.read(data);

        freeSpace.set(getUnallocatedClusters());
        endOfRecordMarker = getEndOfRecordMarker();
        mediaTypeMarker = getMediaTypeMarker();
        data.position(0);
    }

    public FAT16(
            File imageFile,
            int bytesPerFat,
            int fatCopies,
            long fatOffset,
            int bytesPerCluster,
            int clusterCount,
            int rootDirectoryEntries,
            long rootDirectoryOffset,
            long dataOffset) throws IOException {
        try (var imageChannel = FileChannel.open(imageFile.toPath())) {
            this.imageFile = imageFile;
            this.bytesPerFat = bytesPerFat;
            this.fatCopies = fatCopies;
            this.fatOffset = fatOffset;
            this.bytesPerCluster = bytesPerCluster;
            this.clusterCount = clusterCount;
            this.rootDirectoryEntries = rootDirectoryEntries;
            this.rootDirectoryOffset = rootDirectoryOffset;
            this.dataOffset = dataOffset;
            imageChannel.position(fatOffset);
            data = ByteBuffer.allocate(bytesPerFat);
            imageChannel.read(data);

            freeSpace.set(getUnallocatedClusters());
            endOfRecordMarker = getEndOfRecordMarker();
            mediaTypeMarker = getMediaTypeMarker();
            data.position(0);
        }
    }

    public FAT16(
            File imageFile,
            int bytesPerFat,
            int fatCopies,
            long fatOffset,
            int bytesPerCluster,
            int clusterCount,
            int rootDirectoryEntries,
            int mediaTypeMarker,
            int endOfRecordMarker) throws IOException {
        this.imageFile = imageFile;
        this.bytesPerFat = bytesPerFat;
        this.fatCopies = fatCopies;
        this.fatOffset = fatOffset;
        this.bytesPerCluster = bytesPerCluster;
        this.clusterCount = clusterCount;
        this.rootDirectoryEntries = rootDirectoryEntries;
        this.endOfRecordMarker = endOfRecordMarker;
        this.rootDirectoryOffset = fatOffset + ((long) bytesPerFat * fatCopies);
        this.dataOffset = rootDirectoryOffset + ((long) rootDirectoryEntries * DirectoryItemEntry.BYTES_PER_DIRECTORY_ENTRY);
        this.mediaTypeMarker = mediaTypeMarker;
        data = ByteBuffer.allocate(bytesPerFat);
        initialize();

        freeSpace.set(getUnallocatedClusters());
        data.position(0);
    }

    private void initialize() throws IOException {
        try(var channel = openForWrite()) {
            channel.position(fatOffset);
            Arrays.fill(data.array(), (byte)0);
            var mediaTypeAndEndOfRecord = new byte[4];
            ByteUtils.writeWord(mediaTypeAndEndOfRecord, mediaTypeMarker, 0);
            ByteUtils.writeWord(mediaTypeAndEndOfRecord, endOfRecordMarker, 2);
            commit(channel);
            channel.position(rootDirectoryOffset);
            byte[] blankEntry = new byte[32];
            Arrays.fill(blankEntry, (byte)0);
            for(int i = 0; i < this.rootDirectoryEntries; i++) {
                channel.write(ByteBuffer.wrap(blankEntry));
            }
        }
    }

    /**
     * removes the requested amount of space from the free space pool and returns the list of cluster numbers to be used
     * for storage.  This operation does not update the underlying data, though any free space allocation done
     * through the same FAT16 instance will not use the cluster numbers already returned from allocate.
     * @param fileSize The size of the data that will be persisted from disk
     * @return a list of cluster numbers of the corresponding clusters to set.
     */
    public List<Integer> allocate(long fileSize) {
        int clusterCount = (int) (fileSize / bytesPerCluster);
        if(fileSize - ((long)clusterCount * bytesPerCluster) > 0) {
            clusterCount++;
        }
        return allocate(clusterCount);
    }

    public List<Integer> allocate(int clusterCount) {
        int freeCusterCount = freeSpace.get().getSize();
        if(clusterCount > freeCusterCount) {
            throw new IllegalArgumentException(String.format("requested %d clusters but only %d are free", clusterCount, freeCusterCount));
        }

        ClusterList freespaceHead;
        ClusterList newHead;

        /*
            Critical section: advances the newHead pointer until enough unallocated clusters
            have been found and then attempts to set the free space linked list to the new head
            value.  If the free space list has already been altered before this operation completes,
            subsequent attempts will be made.
         */
        do {
            freespaceHead = freeSpace.get();
            newHead = freespaceHead;
            for(int x = 0; x < clusterCount; x++) {
                newHead = freespaceHead.getNext();
            }
            newHead.setSize(freespaceHead.getSize() - clusterCount);
        } while(!freeSpace.compareAndSet(freespaceHead, newHead));

        List<Integer> result = new ArrayList<>(clusterCount);

        while(freespaceHead != newHead) {
            result.add(freespaceHead.getClusterNumber());
            freespaceHead = freespaceHead.getNext();
        }
        return result;
    }

    private FileChannel openForWrite() throws IOException {
        return FileChannel.open(imageFile.toPath(), Set.of(StandardOpenOption.READ, StandardOpenOption.WRITE, StandardOpenOption.SYNC));
    }

    public void commit() throws IOException {
        try(var channel = openForWrite()) {
            commit(channel);
        }
    }

    private void commit(FileChannel channel) throws IOException {
        for (int i = 0; i < fatCopies; i++) {
            commit(channel, fatOffset + ((long) bytesPerFat * i));
        }
    }

    public void store(List<Integer> clusterNumbers) {
        for (int i = 0; i < clusterNumbers.size(); i++) {
            int currentClusterNumber = clusterNumbers.get(i);
            int nextClusterNumber = i + 1 < clusterNumbers.size() ? clusterNumbers.get(i + 1) : endOfRecordMarker;
            data.position((currentClusterNumber * BYTES_PER_ENTRY));
            byte low = (byte)(nextClusterNumber & 0xFF);
            byte high = (byte)((nextClusterNumber & 0xFF00) >> 8);
            data.put(new byte[] { high, low });
        }
    }

    public void free(List<Integer> clusterNumbers) {
        ClusterList freespaceHead;
        for(var clusterNumber : clusterNumbers) {
            data.position((clusterNumber * BYTES_PER_ENTRY));
            data.putShort((short) 0);
        }
        ClusterList newFreespace;
        do {
            freespaceHead = freeSpace.get();
            newFreespace = getUnallocatedClusters();
        } while(!freeSpace.compareAndSet(freespaceHead, newFreespace));
    }

    private void commit(FileChannel imageFileChannel, long fatOffset) throws IOException {
        imageFileChannel.position(fatOffset);
        data.position(0);
        imageFileChannel.write(data);
    }

    public long getFreeSpace() {
        return (long) freeSpace.get().getSize() * bytesPerCluster;
    }

    private ClusterList getUnallocatedClusters() {
        data.position(2 * BYTES_PER_ENTRY);
        int maxPosition = (clusterCount + 1) * BYTES_PER_ENTRY;
        int clusterNumber = 2;
        ClusterList head = null;
        ClusterList current = null;
        byte[] dword = new byte[2];
        while(data.position() < maxPosition) {
            data.get(dword);
            if(dword[0] == 0 && dword[1] == 0) {
                if(head == null) {
                    head = new ClusterList(clusterNumber);;
                    current = head;
                } else {
                    current.setNext(new ClusterList(clusterNumber));
                    current = current.getNext();
                    head.incrementSize();
                }
            }
            clusterNumber++;
        }
        return head;
    }

    private int getEndOfRecordMarker() {
        return (int) parseLong(data, 2, 2);
    }

    private int getMediaTypeMarker() {
        return (int) parseLong(data, 0, 2);
    }

    /**
     * Returns a tuple with the first entry being the index of the sector of the FAT containing the FAT entry
     * for a particular cluster, and the
     * @param clusterNumber
     * @return
     */
    public int getClusterEntryLocation(int clusterNumber) {
        return clusterNumber * BYTES_PER_ENTRY;
    }

    public int getClusterEntry(int clusterEntryCoordinates) {
        return (int) parseLong(data, clusterEntryCoordinates, 2);
    }

    public int getNextClusterNumber(int clusterNumber) {
        return getClusterEntry(getClusterEntryLocation(clusterNumber));
    }

    public List<Integer> getClusters(int firstCluster) {
        List<Integer> clusterChain = new LinkedList<>();
        int cluster = firstCluster;
        do {
            clusterChain.add(cluster);
            cluster = getNextClusterNumber(cluster);
        } while (cluster != getEndOfRecordMarker());
        return clusterChain;
    }

    private long getClusterPosition(int clusterNumber) {
        long clusterOffset = (long) (clusterNumber - 2) * bytesPerCluster;
        return dataOffset + clusterOffset;
    }

    public ByteBuffer readCluster(int clusterNumber) throws IOException {
        if(clusterNumber < 2) {
            throw  new IllegalArgumentException("Cluster Number must be 2 or greater");
        }

        try(var channel = FileChannel.open(imageFile.toPath())) {
            channel.position(getClusterPosition(clusterNumber));
            var result = ByteBuffer.allocate(bytesPerCluster);
            channel.read(result);
            return result;
        }
    }

    public void writeCluster(ByteBuffer clusterData, int clusterNumber) throws IOException {
        try(var channel = FileChannel.open(imageFile.toPath(), Set.of(StandardOpenOption.READ, StandardOpenOption.WRITE, StandardOpenOption.SYNC))) {
            clusterData.position(0);
            var toWrite = clusterData.slice(0, bytesPerCluster);
            channel.position(getClusterPosition(clusterNumber));
            channel.write(toWrite);
        }
    }

    @Override
    public DirectoryBase getRootDirectory() throws IOException {
        return new RootDirectory(this, getRootDirectoryEntries(), rootDirectoryEntries);
    }

    private List<DirectoryItemEntry> getRootDirectoryEntries() throws IOException {
        return DirectoryItemEntry.fromBuffer(readRootDirectory(), 0).toList();
    }

    private ByteBuffer readRootDirectory() throws IOException {
        try(var channel = FileChannel.open(imageFile.toPath())) {
            channel.position(rootDirectoryOffset);
            var result = ByteBuffer.allocate(rootDirectoryEntries * DirectoryItemEntry.BYTES_PER_DIRECTORY_ENTRY);
            channel.read(result);
            return result;
        }
    }
}
