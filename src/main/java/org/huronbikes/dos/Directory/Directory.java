package org.huronbikes.dos.Directory;

import java.io.IOException;
import java.util.List;
import java.util.stream.Stream;

public interface Directory {
    Stream<DirectoryItemEntry> getDirectoryEntries() throws IOException;

    List<DirectoryItemEntry> list() throws IOException;

    DirectoryBase getSubDirectory(DirectoryItemEntry item) throws IOException;

    DirectoryBase getSubDirectory(String directory) throws IOException;
    int getFirstClusterNumber();
    int getLastClusterNumber();
}
