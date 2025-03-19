package org.huronbikes.dos.Directory;

import lombok.Getter;
import org.huronbikes.dos.FAT.FAT;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.List;
import java.util.Queue;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class SubDirectory extends DirectoryBase implements Directory {
    private final List<Integer> clusters;
    @Getter
    private final String volumeLabel;

    private record SubDirectoryEntry(int clusterNumber, DirectoryItemEntry entry) {}

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
    public Stream<DirectoryItemEntry> getDirectoryEntries() throws IOException {
        return stream().map(SubDirectoryEntry::entry);
    }

    private Stream<SubDirectoryEntry> stream() {
        return fat.getClusters(clusters.getFirst())
                .stream()
                .flatMap(clusterNumber -> {
                    try {
                        var cluster = fat.readCluster(clusterNumber);
                        return DirectoryItemEntry.fromBuffer(cluster, clusterNumber)
                                .map(entry -> new SubDirectoryEntry(clusterNumber, entry));
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
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
        var items = DirectoryItemEntry.fromBuffer(tailClusterDirectoryEntries, getLastClusterNumber()).toList();
        if((items.size() + 1) * DirectoryItemEntry.BYTES_PER_DIRECTORY_ENTRY > fat.getBytesPerCluster()) {
            tailClusterDirectoryEntries = ByteBuffer.allocate(fat.getBytesPerCluster());
            var nextDirectoryClusters = fat.allocate(1);
            fat.store(List.of(getLastClusterNumber(), nextDirectoryClusters.getFirst()));
            clusters.add(nextDirectoryClusters.getFirst());
        } else {
            tailClusterDirectoryEntries.position(items.size() * DirectoryItemEntry.BYTES_PER_DIRECTORY_ENTRY);
        }
        item.writeDirectoryEntry(directoryEntryBuffer);
        tailClusterDirectoryEntries.put(directoryEntryBuffer);
        fat.writeCluster(tailClusterDirectoryEntries, getLastClusterNumber());
    }

    public void removeDirectoryEntry(DirectoryItemEntry item) throws IOException {
        if(item.getName().equals(CURRENT_DIRECTORY_NAME) || item.getName().equals(PARENT_DIRECTORY_NAME)) {
            throw new IllegalArgumentException("Cannot remove the current/parent directory references");
        }

        ByteBuffer clusterBuffer = ByteBuffer.allocate(fat.getBytesPerCluster());
        Arrays.fill(clusterBuffer.array(), (byte)0);
        clusterBuffer.position(0);

        Queue<Integer> clusterQueue = new ArrayDeque<>(fat.getClusters(getFirstClusterNumber()));
        byte[] entrybuffer = new byte[DirectoryItemEntry.BYTES_PER_DIRECTORY_ENTRY];
        try {
            stream().filter(sde -> !sde.entry.matches(item))
                    .forEach(sde -> {
                        sde.entry.writeDirectoryEntry(entrybuffer);
                        clusterBuffer.put(entrybuffer);
                        if (!clusterBuffer.hasRemaining()) {
                            try {
                                fat.writeCluster(clusterBuffer, clusterQueue.remove());
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                            Arrays.fill(clusterBuffer.array(), (byte)0);
                            clusterBuffer.position(0);
                        }
                    });
        } catch (RuntimeException re) {
            if(re.getCause() instanceof IOException ioException) {
                throw ioException;
            } else {
                throw re;
            }
        }

        if (clusterBuffer.position() > 0) {
            fat.writeCluster(clusterBuffer, clusterQueue.remove());
        }

        if(!clusterQueue.isEmpty()) {
            fat.store(clusters.stream().filter(c -> !clusterQueue.contains(c)).toList());
            fat.free(clusterQueue.stream().toList());
            fat.commit();
        }
    }


}
