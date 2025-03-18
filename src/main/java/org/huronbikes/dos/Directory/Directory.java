package org.huronbikes.dos.Directory;

import java.io.IOException;
import java.util.List;

public interface Directory {
    List<DirectoryItemEntry> getDirectoryEntries() throws IOException;

    List<DirectoryItemEntry> list() throws IOException;

    DirectoryBase getSubDirectory(DirectoryItemEntry item) throws IOException;

    DirectoryBase getSubDirectory(String directory) throws IOException;
    int getFirstClusterNumber();
    int getLastClusterNumber();
}
