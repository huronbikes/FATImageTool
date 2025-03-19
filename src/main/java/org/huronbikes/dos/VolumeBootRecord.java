package org.huronbikes.dos;

import lombok.Getter;
import org.huronbikes.dos.Directory.DirectoryItemEntry;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.huronbikes.dos.ByteUtils.parseLong;

/**
 * VolumeBootRecord for FAT 16 file systems
 */
@Getter
public class VolumeBootRecord {
    private static final List<Integer> VALID_BYTES_PER_SECTOR = List.of(512, 1024, 2048, 4096);
    private static final String VALID_BYTES_PER_SECTOR_STRING = VALID_BYTES_PER_SECTOR
            .stream()
            .map(i -> Integer.toString(i))
            .collect(Collectors.joining(", "));
    ByteBuffer data;
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
        return (((long) numberOfRootEntries * DirectoryItemEntry.BYTES_PER_DIRECTORY_ENTRY) + (bytesPerSector - 1)) / bytesPerSector;
    }

    public long getDataSectorCount() {
        return getTotalSectorCount() - (reservedSectors + ((long) numberOfFatCopies * sectorsPerFat) + getRootDirectorySectorCount());
    }

    public VolumeBootRecord(ByteBuffer data) {
        this.data = data;
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
        data.position(0);
        jumpInstruction = ByteUtils.readAll(data.slice(0, 3));
        vendorString = ByteUtils.readAll(data.slice(3,8));
        bytesPerSector = (int) parseLong(data.slice(11, 2));
        sectorsPerCluster = BYTE_MASK & data.get(13);
        reservedSectors = (int) parseLong(data.slice(14, 2));
        numberOfFatCopies = BYTE_MASK & data.get(16);
        numberOfRootEntries = (int) parseLong(data.slice(17,2));
        sectorCount = (int) parseLong(data.slice(19,2));
        mediaTypeIndicator = MediaType.getIndicator(BYTE_MASK & data.get(21), MediaType.DiskType.FIXED);
        sectorsPerFat = (int) parseLong(data.slice(22,2));
        sectorsPerTrack = (int) parseLong(data.slice(24,2));
        numberOfHeads = (int) parseLong(data.slice(26, 2));
        numberOfHiddenSectors = parseLong(data.slice(28,4));
        dwordSectorCount = parseLong(data.slice(32, 4));
        logicalDriveNumber = BYTE_MASK & data.get(36);
        dwordSectorsPerFat = parseLong(data.slice(36, 4));
        extendedSignature = BYTE_MASK & data.get(38);
        if(extendedSignature == EXTENDED_SIGNATURE) {
            partitionSerialNumber = parseLong(data.slice(39,4));
            volumeLabel = ByteUtils.readAll(data.slice(43,11));
            fileSystemType = ByteUtils.readAll(data.slice(54,8));
        }
    }

    public void setJumpInstruction(byte[] jumpInstruction) {
        if(jumpInstruction.length != 3) {
            throw new IllegalArgumentException("Jump instruction must be 3 bytes.");
        }
        this.jumpInstruction = jumpInstruction;
        data.put(0, jumpInstruction, 0, 3);
    }

    public void setVendorString(String vendorString) {
        var vendorByteString = vendorString.toUpperCase().getBytes(StandardCharsets.US_ASCII);
        data.put(3, vendorByteString, 0, 8);
        this.vendorString = Arrays.copyOfRange(vendorByteString, 0, 8);
    }

    public void setBytesPerSector(int bytesPerSector) {
        if(!VALID_BYTES_PER_SECTOR.contains(bytesPerSector)) {
            throw new IllegalArgumentException(String.format("%d is not a valid bytes-per-sector value.  Must be one of %s", bytesPerSector,
VALID_BYTES_PER_SECTOR_STRING));
        }
        putWord(bytesPerSector, 11);
        this.bytesPerSector = bytesPerSector;
    }

    public void setSectorsPerCluster(int sectorsPerCluster) {
        data.put(13, (byte) sectorsPerCluster);
        this.sectorsPerCluster = BYTE_MASK & sectorsPerCluster;
    }

    public void setReservedSectors(int reservedSectors) {
        putWord(reservedSectors, 14);
        this.reservedSectors = reservedSectors;
    }

    public void setNumberOfFatCopies(int fatCopies) {
        data.put(16, (byte) fatCopies);
        this.numberOfFatCopies = BYTE_MASK & fatCopies;
    }

    public void setNumberOfRootEntries(int numberOfRootEntries) {
        putWord(numberOfRootEntries, 17);
        this.numberOfRootEntries = numberOfRootEntries;
    }

    public void setSectorCount(int sectorCount) {
        putWord(sectorCount, 19);
        this.sectorCount = sectorCount;
    }

    public void setMediaTypeIndicator(MediaType.Indicator indicator) {
        data.put(21, (byte)indicator.getValue());
        this.mediaTypeIndicator = indicator;
    }

    public void setSectorsPerFat(int sectorsPerFat) {
        putWord(sectorsPerFat, 22);
        this.sectorsPerFat = sectorsPerFat;
    }

    public void setSectorsPerTrack(int sectorsPerTrack) {
        putWord(sectorsPerTrack, 24);
        this.sectorsPerTrack = sectorsPerTrack;
    }

    public void setNumberOfHeads(int numberOfHeads) {
        putWord(numberOfHeads, 26);
        this.numberOfHeads = numberOfHeads;
    }

    public void setNumberOfHiddenSectors(long numberOfHiddenSectors) {
        putDWord(numberOfHiddenSectors, 28);
        this.numberOfHiddenSectors = numberOfHiddenSectors;
    }

    public void setDwordSectorCount(long dwordSectorCount) {
        putDWord(dwordSectorCount, 32);
        this.dwordSectorCount = dwordSectorCount;
    }

    public void setLogicalDriveNumber(int logicalDriveNumber) {
        data.put(36, (byte) logicalDriveNumber);
        this.logicalDriveNumber = logicalDriveNumber;
    }

    public void setExtendedSignature(int extendedSignature) {
        data.put(38, (byte) extendedSignature);
        this.extendedSignature = BYTE_MASK & extendedSignature;
    }

    public void setPartitionSerialNumber(long partitionSerialNumber) {
        putDWord(partitionSerialNumber, 39);
        this.partitionSerialNumber = partitionSerialNumber;
    }

    public void setVolumeLabel(byte[] volumeLabel) {
        this.volumeLabel = Arrays.copyOfRange(volumeLabel, 0, 11);
        data.put(43, volumeLabel, 0, 11);
    }

    public void setFileSystemType(byte[] fileSystemType) {
        this.fileSystemType = Arrays.copyOfRange(fileSystemType, 0, 8);
        data.put(54, fileSystemType, 0, 8);
    }

    private void putWord(int wordValue, int index) {
        var word = new byte[2];
        ByteUtils.writeWord(word, wordValue, 0);
        data.put(index, word, 0, 2);
    }

    private void putDWord(long dwordValue, int index) {
        var dword = new byte[4];
        ByteUtils.writeDWord(dword, dwordValue, 0);
        data.put(index, dword, 0, 4);
    }
}
