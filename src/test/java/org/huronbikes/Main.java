package org.huronbikes;

import org.huronbikes.dos.Directory.DirectoryItemEntry;
import org.huronbikes.dos.FAT.FAT;
import org.huronbikes.dos.MasterBootRecord;
import org.huronbikes.dos.Sector;
import org.huronbikes.dos.VolumeBootRecord;
import org.huronbikes.dos.VolumeContext;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

public class Main {
    public static void main(String[] args) throws IOException {
        File file = new File("/Users/andrew/code/MS-DOS/fd.img");
        try(var stream = Files.newInputStream(file.toPath())) {
            MasterBootRecord mbr = Sector.atPosition(stream, 0, MasterBootRecord::new);
            int vbrSector = (int) mbr.getPartitionTableEntry(0).getLbaStart();
            stream.skip((vbrSector - 1) * 512);
            VolumeBootRecord volumeBootRecord = new VolumeBootRecord(stream);
            FAT fat = VolumeContext.getFAT(volumeBootRecord, file);
            var root = fat.getRootDirectory();
            System.out.printf("Volume Name is %s%n", root.getVolumeLabel());
            List<DirectoryItemEntry> dir = root.list();
            dir.forEach(e ->
                    System.out.printf(
                            "+%s %s %s %s %s %d %d%n",
                            e.getName(),
                            e.getCreationTime(),
                            e.getWriteTime(),
                            e.getLastAccessDate(),
                            e.getAttributes().toShortString(),
                            e.getFirstCluster(),
                            e.getFileSize()
                            ));

            var src = root.getSubDirectory("TEST");
            src.list().forEach(e ->
                    System.out.printf(
                            "+--%s %s %s %s %s %d %d%n",
                            e.getName(),
                            e.getCreationTime(),
                            e.getWriteTime(),
                            e.getLastAccessDate(),
                            e.getAttributes().toShortString(),
                            e.getFirstCluster(),
                            e.getFileSize()
                            ));
        }
    }
}