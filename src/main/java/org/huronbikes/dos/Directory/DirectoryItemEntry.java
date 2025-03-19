package org.huronbikes.dos.Directory;

import lombok.*;
import org.huronbikes.dos.ByteUtils;

import java.nio.ByteBuffer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

@AllArgsConstructor
public class DirectoryItemEntry {
    public static final int BYTES_PER_DIRECTORY_ENTRY = 32;
    private static final int BYTES_PER_SHORT_NAME = 11;
    private static final int ATTRIBUTES_OFFSET = 11;
    private static final int CREATION_TIME_TENTHS_OFFSET = 13;
    private static final int CREATION_TIME_OFFSET = 14;
    private static final int CREATION_DATE_OFFSET = 16;
    private static final int LAST_ACCESS_DATE_OFFSET = 18;
    private static final int FIRST_CLUSTER_HI_OFFSET = 20;
    private static final int WRITE_TIME_OFFSET = 22;
    private static final int WRITE_DATE_OFFSET = 24;
    private static final int FIRST_CLUSTER_LO_OFFSET = 26;
    private static final int FILE_SIZE_OFFSET = 28;

    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Data
    public static class Attributes {
        private boolean readOnly;
        private boolean hidden;
        private boolean system;
        private boolean volumeId;
        private boolean directory;
        private boolean archive;

        public byte toByte() {
            boolean[] flags = new boolean[] {readOnly, hidden, system, volumeId, directory, archive};
            int result = 0;
            for(int i = 0; i < flags.length; i++) {
                if(flags[i]) {
                    result += 1 << i;
                }
            }
            return (byte) result;
        }

        public static Attributes fromByte(byte attributeSet) {
            return Attributes.builder()
                    .readOnly((attributeSet & 0x1) == 0x1)
                    .hidden((attributeSet & 0x2) == 0x2)
                    .system((attributeSet & 0x4) == 0x4)
                    .volumeId((attributeSet & 0x8) == 0x8)
                    .directory((attributeSet & 0x10) == 0x10)
                    .archive((attributeSet & 0x20) == 0x20)
                    .build();
        }

        public String toShortString() {
            StringBuilder builder = new StringBuilder(6);
            BiConsumer<Character, Boolean> setFlag = (c, f) -> {
                if(f) {
                    builder.append(c);
                } else {
                    builder.append('-');
                }
            };

            setFlag.accept('R', readOnly);
            setFlag.accept('H', hidden);
            setFlag.accept('S', system);
            setFlag.accept('V', volumeId);
            setFlag.accept('D', directory);
            setFlag.accept('A', archive);
            return builder.toString();
        }
    }

    @Getter
    private final byte[] shortFileName;
    @Getter
    private final Attributes attributes;
    private final byte reserved = 0;
    // file creation time tenths of a second component.
    // I'm sure there's a fascinating story behind this...
    private final byte fileCreationTenths;
    private final int fileCreationTime;
    private final int fileCreationDate;
    private final int fileLastAccessDate;
    @Getter
    private final int firstCluster;
    private final int writeTime;
    private final int writeDate;
    @Getter
    private final long fileSize;
    private final int entryCluster;
    private final int entryOffset;

    public String getName() {
        if(attributes.isVolumeId()) {
            int space = 0;
            for (; space < 11 && shortFileName[space] != (byte) 32; space++);
            return ByteUtils.asciiString(shortFileName, 0, space);
        } else {
            StringBuilder builder = new StringBuilder();
            int space = 0;
            for (; space < 8 && shortFileName[space] != (byte) 32; space++) ;
            builder.append(ByteUtils.asciiString(shortFileName, 0, space));
            for (space = 8; space < 11 && shortFileName[space] != (byte) 32; space++) ;
            if (space > 8) {
                builder.append('.');
                builder.append(ByteUtils.asciiString(shortFileName, 8, space - 8));
            }
            return builder.toString();
        }
    }

    public FileDateTime getWriteTime()  {
        return new FileDateTime(writeDate, writeTime);
    }

    public FileDateTime getCreationTime() {
        return new FileDateTime(fileCreationDate, fileCreationTime);
    }

    public FileDateTime getLastAccessDate() {
        return new FileDateTime(fileLastAccessDate);
    }

    public static DirectoryItemEntry createNew(            String name,
                                                           Attributes attributes,
                                                           LocalDateTime fileCreationTime,
                                                           LocalDateTime writeTime,
                                                           int firstCluster,
                                                           long fileSize) {
        return new DirectoryItemEntry(name, attributes, fileCreationTime, writeTime, firstCluster, fileSize);
    }

    private DirectoryItemEntry(
            String name,
            Attributes attributes,
            LocalDateTime fileCreationTime,
            LocalDateTime writeTime,
            int firstCluster,
            long fileSize
    ) {
        this(name, attributes, fileCreationTime, writeTime, null, firstCluster, fileSize);
    }

    public boolean isPersisted() {
        return this.entryCluster != -1 && this.entryOffset != -1;
    }

    private DirectoryItemEntry(
            String name,
            Attributes attributes,
            LocalDateTime fileCreationTime,
            LocalDateTime writeTime,
            LocalDate lastAccessed,
            int firstCluster,
            long fileSize
    ) {
        if(!name.equals(DirectoryBase.CURRENT_DIRECTORY_NAME) && !name.equals(DirectoryBase.PARENT_DIRECTORY_NAME)) {
            String[] parts = name.split("\\.");
            if (parts.length == 2) {
                if (parts[0].length() > 8) {
                    throw new IllegalArgumentException("file name should not exceed 8 characters");
                }

                if (parts[1].length() > 3) {
                    throw new IllegalArgumentException("file extension should not exceed 3 characters");
                }

                shortFileName = ByteUtils.shortName(parts[0].toUpperCase(), parts[1].toUpperCase());
            } else if (parts.length == 1) {
                if (parts[0].length() > 8) {
                    throw new IllegalArgumentException("file name should not exceed 8 characters");
                }
                shortFileName = ByteUtils.shortName(parts[0].toUpperCase());
            } else {
                throw new IllegalArgumentException(String.format("%s is not a valid file name", name));
            }
        } else {
            shortFileName = ByteUtils.shortName(name);
        }

        this.attributes = attributes;
        this.fileCreationTenths = 0;
        this.fileCreationTime = FileDateTime.toInteger(fileCreationTime);
        this.fileCreationDate = FileDateTime.toInteger(fileCreationTime.toLocalDate());

        this.writeTime = FileDateTime.toInteger(writeTime);
        this.writeDate = FileDateTime.toInteger(writeTime.toLocalDate());

        this.fileLastAccessDate = FileDateTime.toInteger(lastAccessed);
        this.firstCluster = firstCluster;
        this.fileSize = fileSize;
        this.entryCluster = -1;
        this.entryOffset = -1;
    }

    private DirectoryItemEntry(byte[] bytes, int clusterNumber, int entryOffset) {
        this(bytes, 0, clusterNumber, entryOffset);
    }

    private DirectoryItemEntry(byte[] bytes, int offset, int clusterNumber, int entryOffset) {
        this.shortFileName = Arrays.copyOfRange(bytes, offset, offset + BYTES_PER_SHORT_NAME);
        this.attributes = Attributes.fromByte(bytes[offset + ATTRIBUTES_OFFSET]);
        this.fileCreationTenths = bytes[offset + CREATION_TIME_TENTHS_OFFSET];
        this.fileCreationTime = (int) ByteUtils.parseLong(bytes, offset + CREATION_TIME_OFFSET, 2);
        this.fileCreationDate = (int) ByteUtils.parseLong(bytes, offset + CREATION_DATE_OFFSET, 2);
        this.fileLastAccessDate = (int) ByteUtils.parseLong(bytes, offset + LAST_ACCESS_DATE_OFFSET, 2);
        int firstCluster = (int)(ByteUtils.parseLong(bytes, offset + FIRST_CLUSTER_HI_OFFSET, 2) << 32);
        firstCluster += (int)ByteUtils.parseLong(bytes, offset + FIRST_CLUSTER_LO_OFFSET, 2);
        this.firstCluster = firstCluster;
        this.writeTime = (int) ByteUtils.parseLong(bytes, offset + WRITE_TIME_OFFSET, 2);
        this.writeDate = (int) ByteUtils.parseLong(bytes, offset + WRITE_DATE_OFFSET, 2);
        this.fileSize = ByteUtils.parseLong(bytes, offset + FILE_SIZE_OFFSET, 4);
        this.entryCluster = clusterNumber;
        this.entryOffset = entryOffset;
    }

    public void writeDirectoryEntry(byte[] target) {
        Arrays.fill(target, (byte)0);
        for (int i = 0; i < shortFileName.length; i++) {
            target[i] = shortFileName[i];
        }
        target[ATTRIBUTES_OFFSET] = attributes.toByte();
        target[CREATION_TIME_TENTHS_OFFSET] = 0;
        ByteUtils.writeWord(target, fileCreationTime, CREATION_TIME_OFFSET);
        ByteUtils.writeWord(target, fileCreationDate, CREATION_DATE_OFFSET);
        ByteUtils.writeWord(target, fileLastAccessDate, LAST_ACCESS_DATE_OFFSET);
        ByteUtils.writeWord(target, (firstCluster >> 16) & 0x3FFF, FIRST_CLUSTER_HI_OFFSET);
        ByteUtils.writeWord(target, writeTime, WRITE_TIME_OFFSET);
        ByteUtils.writeWord(target, writeDate, WRITE_DATE_OFFSET);
        ByteUtils.writeWord(target, (firstCluster & 0xFFFF), FIRST_CLUSTER_LO_OFFSET);
        ByteUtils.writeDWord(target, fileSize, FILE_SIZE_OFFSET);
    }

    public static Stream<DirectoryItemEntry> fromBuffer(ByteBuffer buffer, int clusterNumber) {
        byte[] rawEntry = new byte[BYTES_PER_DIRECTORY_ENTRY];
        int limit = buffer.limit() / BYTES_PER_DIRECTORY_ENTRY;
        buffer.position(0);
        return Stream.generate(() -> {
            int entryOffset = buffer.position();
            buffer.get(rawEntry);
            if(rawEntry[0] != 0) {
                return new DirectoryItemEntry(rawEntry, clusterNumber, entryOffset);
            } else {
                return null;
            }
        }).limit(limit).filter(Objects::nonNull);
    }

    //TODO how do we match entries?
    public boolean matches(DirectoryItemEntry other) {
        return other.firstCluster == this.firstCluster;
    }
}
