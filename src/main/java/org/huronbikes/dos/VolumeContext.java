package org.huronbikes.dos;

import org.huronbikes.dos.FAT.FAT;
import org.huronbikes.dos.FAT.FAT16;

import java.io.File;
import java.io.IOException;

public class VolumeContext {

    //TODO support for FAT12 / FAT32?
    public static FAT getFAT(VolumeBootRecord volumeBootRecord, File imageFile) throws IOException {
        return new FAT16(
                imageFile,
                volumeBootRecord.getBytesPerFat(),
                volumeBootRecord.getNumberOfFatCopies(),
                volumeBootRecord.getFatStartOffset(),
                volumeBootRecord.getBytesPerCluster(),
                volumeBootRecord.getClusterCount(),
                volumeBootRecord.getNumberOfRootEntries(),
                volumeBootRecord.getRootDirOffset(),
                volumeBootRecord.getDataOffset());
    }
}
