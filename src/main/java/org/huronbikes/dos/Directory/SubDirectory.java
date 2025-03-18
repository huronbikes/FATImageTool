package org.huronbikes.dos.Directory;

import lombok.Getter;
import org.huronbikes.dos.FAT.FAT;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;

public class SubDirectory extends DirectoryBase implements Directory {
    private final List<Integer> clusters;
    @Getter
    private final String volumeLabel;

    public SubDirectory(FAT fat, List<Integer> clusters, String volumeLabel) {
        super(fat);
        this.clusters = clusters;
        this.volumeLabel = volumeLabel;
    }

    @Override
    public void addCluster(int clusterNumber) {
        clusters.add(clusterNumber);
    }

    @Override
    public List<DirectoryItemEntry> getDirectoryEntries() throws IOException {
        List<DirectoryItemEntry> result = new LinkedList<>();
        for(var cluster: clusters) {
            var buffer = fat.readCluster(cluster);
            result.addAll(DirectoryItemEntry.getDirectoryEntries(buffer.array(), 0));
        }
        return result;
    }

    @Override
    public DirectoryBase getSubDirectory(DirectoryItemEntry item) throws IOException{
        if (item.getFirstCluster() == 0) {
            return fat.getRootDirectory();
        } else if (item.getFirstCluster() == clusters.getFirst()) {
            return this;
        } else {
            return new SubDirectory(fat, fat.getClusters(item.getFirstCluster()), volumeLabel);
        }
    }

    @Override
    public int getFirstClusterNumber() {
        return clusters.getFirst();
    }

    @Override
    public int getLastClusterNumber() {
        return clusters.getLast();
    }

    public void addDirectoryEntry(DirectoryItemEntry item) throws IOException {
        byte[] directoryEntryBuffer = new byte[DirectoryItemEntry.BYTES_PER_DIRECTORY_ENTRY];
        var tailClusterDirectoryEntries = fat.readCluster(getLastClusterNumber());
        var items = DirectoryItemEntry.getDirectoryEntries(tailClusterDirectoryEntries.array(), 0);
        if((items.size() + 1) * DirectoryItemEntry.BYTES_PER_DIRECTORY_ENTRY > fat.getBytesPerCluster()) {
            tailClusterDirectoryEntries = ByteBuffer.allocate(fat.getBytesPerCluster());
            var nextDirectoryClusters = fat.allocate(1);
            fat.store(List.of(getLastClusterNumber(), nextDirectoryClusters.getFirst()));
            clusters.add(nextDirectoryClusters.getFirst());
        } else {
            tailClusterDirectoryEntries.position(items.size() * DirectoryItemEntry.BYTES_PER_DIRECTORY_ENTRY);
        }
        tailClusterDirectoryEntries.put(directoryEntryBuffer);
        fat.writeCluster(tailClusterDirectoryEntries, getLastClusterNumber());
    }
}
