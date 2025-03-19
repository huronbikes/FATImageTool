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
        int vbrSector = 0;
        try(var stream = Files.newInputStream(file.toPath())) {
            MasterBootRecord mbr = Sector.atPosition(stream, 0, MasterBootRecord::new);
            vbrSector = (int) mbr.getPartitionTableEntry(0).getLbaStart();
        }

        VolumeContext volume = new VolumeContext(file, vbrSector * 512L);
        FAT fat = volume.getFat();
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
        /*
        var test = root.getSubDirectory("test");
        test.list().forEach(e ->
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
        test.makeDirectory("test2");

         */
    }
}