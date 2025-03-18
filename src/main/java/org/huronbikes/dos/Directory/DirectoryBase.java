package org.huronbikes.dos.Directory;

import lombok.RequiredArgsConstructor;
import org.huronbikes.dos.FAT.FAT;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@RequiredArgsConstructor
public abstract class DirectoryBase implements Directory {
    public static final String CURRENT_DIRECTORY_NAME = ".";
    public static final String PARENT_DIRECTORY_NAME = "..";
    protected final FAT fat;

    public abstract int getFirstClusterNumber();
    public abstract int getLastClusterNumber();
    public abstract void addCluster(int newClusterNumber);
    public abstract String getVolumeLabel();

    public abstract List<DirectoryItemEntry> getDirectoryEntries() throws IOException;

    @Override
    public List<DirectoryItemEntry> list() throws IOException {
        return getDirectoryEntries();
    }

    @Override
    public abstract DirectoryBase getSubDirectory(DirectoryItemEntry item) throws IOException;

    @Override
    public DirectoryBase getSubDirectory(String directory) throws IOException {
        var result = list().stream().filter(e -> e.getAttributes().isDirectory()).findFirst();
        if( result.isPresent() ) {
            return getSubDirectory(result.get());
        } else {
            return null;
        }
    }

    public DirectoryBase makeDirectory(String directoryName) throws IOException {
        int bytesPerCluster = fat.getBytesPerCluster();
        if(directoryName.length() > 8) {
            throw new IllegalArgumentException("Directory name must be less than 8 characters");
        }
        var clusters = fat.allocate(1);
        fat.store(clusters);
        ByteBuffer content = ByteBuffer.allocate(fat.getBytesPerCluster());
        LocalDateTime now = LocalDateTime.now();
        DirectoryItemEntry newEntry = new DirectoryItemEntry(
                directoryName,
                DirectoryItemEntry.Attributes.builder().directory(true).build(),
                now,
                now,
                0,
                0L);

        DirectoryItemEntry current = new DirectoryItemEntry(
                CURRENT_DIRECTORY_NAME,
                DirectoryItemEntry.Attributes.builder().directory(true).build(),
                now,
                now,
                0,
                0L);

        DirectoryItemEntry parentEntry = new DirectoryItemEntry(
                PARENT_DIRECTORY_NAME,
                DirectoryItemEntry.Attributes.builder().directory(true).build(),
                now,
                now,
                0,
                0L);

        Arrays.fill(content.array(), (byte)0);
        content.position(0);

        byte[] directoryEntryBuffer = new byte[32];
        Arrays.fill(directoryEntryBuffer, (byte)0);
        current.writeDirectoryEntry(directoryEntryBuffer);
        content.put(directoryEntryBuffer);

        Arrays.fill(directoryEntryBuffer, (byte)0);
        parentEntry.writeDirectoryEntry(directoryEntryBuffer);
        content.put(directoryEntryBuffer);

        addDirectoryEntry(newEntry);
        fat.writeCluster(content, clusters.getFirst());
        fat.commit();
        return getSubDirectory(newEntry);
    }

    protected abstract void addDirectoryEntry(DirectoryItemEntry directoryItemEntry) throws IOException;
}
