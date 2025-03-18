package org.huronbikes.dos;

import java.util.Arrays;

public enum PartitionType {
    EMPTY(0x00),
    FAT12(0x01),
    XENIX_ROOT(0x02),
    XENIX_USR(0x03),
    FAT16_SMALL(0x04),
    EXTENDED_8G(0x05),
    FAT16_BIG(0x06),
    IFS_HPFS_NTFS_EXFAT(0x07),
    LOGICAL_FAT16(0x08),
    AIX_DATA(0x09),
    OS2_BOOT_MANAGER(0x0A),
    WIN95_OSR2_FAT32(0x0B),
    WIN95_OSR2_FAT32_LBA(0x0C),
    SILICON_SAFE(0x0D),
    WIN95_FAT16_LBA(0x0E),
    WIN95_EXTENDED_LBA(0x0F),
    OPUS(0x10),
    HIDDEN_DOS_FAT12(0x11),
    COMPAQ_DIAGNOSTICS(0x12),
    HIDDEN_DOS_FAT16_SMALL(0x14),
    AST_DOS(0x14),
    HIDDEN_DOS_FAT16(0x16),
    HIDDEN_IFS(0x17),
    AST_SMARTSLEEP(0x18),
    HIDDEN_WIN95_OSR2_FAT32(0x1B),
    HIDDEN_WIN95_OSR2_FAT32_LBA(0x1C),
    HIDDEN_WIN95_FAT16_LBA(0x1E),
    NEC_DOS_3_X(0x24),
    WINDOWS_RE_HIDDEN(0x27),
    ATHEOS_FILE_SYSTEM(0x2A),
    SYLLABLESECURE(0x2B),
    NOS(0x32),
    JFS_OS2(0x35),
    THEOS_VER_3_2_2GB(0x38),
    PLAN9(0x39),
    THEOS_4_4GB(0x3A),
    THEOS_4_EXTENDED(0x3B),
    PARTITIONMAGIC_RECOVERY(0x3C),
    UNKNOWN(0xFF);

    private final byte type;
    PartitionType(int type) {
        this.type = (byte)type;
    }

    public byte asByte() {
        return type;
    }

    public static PartitionType fromByte(byte partitionTypeValue) {
        return Arrays.stream(PartitionType.values()).filter(b -> b.asByte() == partitionTypeValue).findFirst().orElse(PartitionType.UNKNOWN);
    }
}
