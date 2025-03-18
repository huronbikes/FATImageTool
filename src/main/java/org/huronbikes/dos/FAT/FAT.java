package org.huronbikes.dos.FAT;

import org.huronbikes.dos.Directory.DirectoryBase;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;

public interface FAT {
    int getBytesPerCluster();
    List<Integer> getClusters(int clusterNumber);
    ByteBuffer readCluster(int clusterNumber) throws IOException;
    void writeCluster(ByteBuffer buffer, int clusterNumber) throws IOException;
    DirectoryBase getRootDirectory() throws IOException;
    List<Integer> allocate(int clusterCount);
    List<Integer> allocate(long fileSize);
    void store(List<Integer> cluster);
    void commit() throws IOException;
}
