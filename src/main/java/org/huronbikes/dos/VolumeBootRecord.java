package org.huronbikes.dos;

import lombok.Getter;
import org.huronbikes.dos.Directory.DirectoryItemEntry;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import static org.huronbikes.dos.ByteUtils.parseLong;

/**
 * VolumeBootRecord for FAT 16 file systems
 */
@Getter
public class VolumeBootRecord extends Sector{
    private static final int EXTENDED_SIGNATURE = 0x29;
    private static final int BYTE_MASK = 0x0FF;
    // BS_jmpBoot
    private byte[] jumpInstruction;
    // BS_OEMName
    private byte[] vendorString;
    // BPB_BytesPerSec
    private int bytesPerSector;
    // BPB_SecPerClus
    private int sectorsPerCluster;
    // BPB_RsvdSecCnt
    private int reservedSectors;
    // BPB_NumFATs
    private int numberOfFatCopies;
    // BPB_RootEntCnt
    private int numberOfRootEntries;
    // BPB_TotSec16
    private int sectorCount;
    // BPB_Media
    private MediaType.Indicator mediaTypeIndicator;
    // BPB_FATSz16
    private int sectorsPerFat;
    // BPB_SecPerTrk
    private int sectorsPerTrack;
    // BPB_NumHeads
    private  int numberOfHeads;
    // BPB_HiddSec
    private long numberOfHiddenSectors;
    // BPB_TotSec32
    private long dwordSectorCount;
    // BS_DrvNum
    private int logicalDriveNumber;
    // BS_Reserved1
    // BS_BootSig
    private int extendedSignature;
    // BS_VolID
    private long partitionSerialNumber;
    // BS_VolLab
    private byte[] volumeLabel;
    // BS_FilSysType
    private byte[] fileSystemType;
    private long dwordSectorsPerFat;

    public long getTotalSectorCount() {
        if (sectorCount != 0) {
            return sectorCount;
        } else {
            return dwordSectorCount;
        }
    }

    public long getFatSize() {
        if(sectorsPerFat !=0) {
            return sectorsPerFat;
        } else {
            return dwordSectorsPerFat;
        }
    }



    public long getRootDirectorySectorCount() {
        return ((numberOfRootEntries * 32L) + (bytesPerSector - 1)) / bytesPerSector;
    }

    public long getDataSectorCount() {
        return getTotalSectorCount() - (reservedSectors + (numberOfFatCopies * sectorsPerFat) + getRootDirectorySectorCount());
    }

    public VolumeBootRecord(InputStream astream) throws IOException {
        super(astream);
        init();
    }

    public VolumeBootRecord(byte[] bytes) {
        super(bytes);
        init();
    }

    public long getFatStartOffset() {
        var offsetSectorCount = getNumberOfHiddenSectors() + getReservedSectors();
        return offsetSectorCount * getBytesPerSector();
    }

    public int getBytesPerFat() {
        return getSectorsPerFat() * getBytesPerSector();
    }

    public int getClusterCount() {
        return (int)(getDataSectorCount() / getSectorsPerCluster());
    }

    public long getRootDirOffset() {
        long fatSectors = (long) getSectorsPerFat() * getNumberOfFatCopies();
        return getFatStartOffset() + (fatSectors * getBytesPerSector());
    }

    public long getDataOffset() {
        return ((long) getNumberOfRootEntries() * DirectoryItemEntry.BYTES_PER_DIRECTORY_ENTRY) + getRootDirOffset();
    }

    public int getBytesPerCluster() {
        return getSectorsPerCluster() * getBytesPerSector();
    }

    protected void init() {
        ByteBuffer dataBuffer = ByteBuffer.wrap(data);
        jumpInstruction = readAll(dataBuffer.slice(0, 3));
        vendorString = readAll(dataBuffer.slice(3,8));
        bytesPerSector = (int) parseLong(dataBuffer.slice(11, 2));
        sectorsPerCluster = BYTE_MASK & dataBuffer.get(13);
        reservedSectors = (int) parseLong(dataBuffer.slice(14, 2));
        numberOfFatCopies = BYTE_MASK & dataBuffer.get(16);
        numberOfRootEntries = (int) parseLong(dataBuffer.slice(17,2));
        sectorCount = (int) parseLong(dataBuffer.slice(19,2));
        mediaTypeIndicator = MediaType.getIndicator(BYTE_MASK & dataBuffer.get(21), MediaType.DiskType.FIXED);
        sectorsPerFat = (int) parseLong(dataBuffer.slice(22,2));
        sectorsPerTrack = (int) parseLong(dataBuffer.slice(24,2));
        numberOfHeads = (int) parseLong(dataBuffer.slice(26, 2));
        numberOfHiddenSectors = parseLong(dataBuffer.slice(28,4));
        dwordSectorCount = parseLong(dataBuffer.slice(32, 4));
        logicalDriveNumber = BYTE_MASK & dataBuffer.get(36);
        dwordSectorsPerFat = parseLong(dataBuffer.slice(36, 4));
        extendedSignature = BYTE_MASK & dataBuffer.get(38);
        if(extendedSignature == EXTENDED_SIGNATURE) {
            partitionSerialNumber = parseLong(dataBuffer.slice(39,4));
            volumeLabel = readAll(dataBuffer.slice(43,11));
            fileSystemType = readAll(dataBuffer.slice(54,8));
        }
    }

    public void setJumpInstruction(byte[] jumpInstruction) {
        if(jumpInstruction.length != 3) {
            throw new IllegalArgumentException("Jump instruction must be 3 bytes.");
        }
        this.jumpInstruction = jumpInstruction;
        writeToData(jumpInstruction, 0, 3);
    }
}
