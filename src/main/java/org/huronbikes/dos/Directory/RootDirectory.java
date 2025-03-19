package org.huronbikes.dos.Directory;

import org.huronbikes.dos.FAT.FAT;
import org.huronbikes.dos.FAT.FAT16;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Stream;

/**
 * Represents a FAT16 Root Directory.  FAT16 root directories occupy the disk space
 * between the FAT portion and the data portion of a disk.
 * Subdirectories that have the Root Directory as a direct parent will have a cluster number of
 * 0 as the cluster number of that entry.
 */
public class RootDirectory extends DirectoryBase {
    private final List<DirectoryItemEntry> directoryEntries;
    private final int maximumEntryCount;

    @Override
    public int getFirstClusterNumber() {
        return 0;
    }

    @Override
    public int getLastClusterNumber() {
        return 0;
    }

    @Override
    public void addCluster(int clusterNumber) {
        throw new RuntimeException("Cannot add a new cluster to the root directory");
    }

    @Override
    public Stream<DirectoryItemEntry> getDirectoryEntries() {
        return directoryEntries.stream();
    }

    public RootDirectory(FAT fat, List<DirectoryItemEntry> directoryEntries, int maximumEntryCount) {
        super(fat);
        this.directoryEntries = directoryEntries;
        this.maximumEntryCount = maximumEntryCount;
    }

    private final static String DEFAULT_VOLUME_LABEL = "NO_VM_LABEL";

    public String getVolumeLabel() {
        var maybeVolumeLabelEntry = getDirectoryEntries()
                .filter(e -> e.getAttributes().isVolumeId()).findFirst();
        return maybeVolumeLabelEntry.map(entry -> new String(entry.getShortFileName(), StandardCharsets.US_ASCII))
                .orElse(DEFAULT_VOLUME_LABEL);
    }

    @Override
    public DirectoryBase getSubDirectory(DirectoryItemEntry item) throws IOException {
        if (item.getFirstCluster() == 0) {
            return this;
        } else {
            return new SubDirectory(fat, fat.getClusters(item.getFirstCluster()), getVolumeLabel());
        }
    }

    @Override
    protected void addDirectoryEntry(DirectoryItemEntry directoryItemEntry) throws IOException {
        if(directoryEntries.size() + 1 > maximumEntryCount) {
            throw new IllegalArgumentException("Cannot add new directory entry, root directory already contains the maximum amount of entries.");
        }
        if(fat instanceof FAT16 fat16) {
            directoryEntries.add(directoryItemEntry);
            fat16.writeRootDirectory(directoryEntries);
        } else {
            throw new IllegalStateException("Root Directory operations not supported by the current filesystem.");
        }
    }

    @Override
    protected void removeDirectoryEntry(DirectoryItemEntry directoryItemEntry) throws IOException {
        var clusterChain = fat.getClusters(directoryItemEntry.getFirstCluster());
        if(fat instanceof FAT16 fat16) {
            directoryEntries.remove(directoryItemEntry);
            fat16.writeRootDirectory(directoryEntries);
        } else {
            throw new IllegalStateException("Root Directory operations not supported by the current filesystem.");
        }
        fat.free(clusterChain);
        fat.commit();
    }
}
