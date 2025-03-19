package org.huronbikes.dos;

import lombok.Getter;
import org.huronbikes.dos.FAT.FAT;
import org.huronbikes.dos.FAT.FAT16;

import java.awt.image.DataBuffer;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.StandardOpenOption;
import java.util.Set;

public class VolumeContext {
    private final File imageFile;
    private final VolumeBootRecord volumeBootRecord;
    @Getter
    private final FAT fat;

    public VolumeContext(File imageFile, long volumeBootRecordOffset) throws IOException {
        this.imageFile = imageFile;
        try(var channel = openForWrite()) {
            channel.position(volumeBootRecordOffset);
            ByteBuffer vbrBuffer = ByteBuffer.allocate(4096);
            channel.read(vbrBuffer);
            volumeBootRecord = new VolumeBootRecord(vbrBuffer);
            fat = new FAT16(
                    imageFile,
                    channel,
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

    private FileChannel openForWrite() throws IOException {
        return FileChannel.open(imageFile.toPath(), Set.of(StandardOpenOption.READ, StandardOpenOption.WRITE, StandardOpenOption.SYNC));
    }


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
