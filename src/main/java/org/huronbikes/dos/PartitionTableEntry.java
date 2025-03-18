package org.huronbikes.dos;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import static org.huronbikes.dos.ByteUtils.parseLong;

@Data
@AllArgsConstructor
public class PartitionTableEntry {
    private static int ACTIVE_PARTITION = 0x80;
    private static int PARTITION_TABLE_ENTRY_LENGTH = 16;
    private boolean active;
    private CHS start;
    private CHS end;
    private PartitionType partitionType;
    private long lbaStart;
    private long sectorLength;

    public static PartitionTableEntry fromBytes(byte[] rawPartitionTableEntry) {
        return fromByteBuffer(ByteBuffer.wrap(rawPartitionTableEntry));
    }

    public static PartitionTableEntry fromByteBuffer(ByteBuffer rawPartitionTableEntry) {
        boolean active = ((int)rawPartitionTableEntry.get(0) & ACTIVE_PARTITION) == ACTIVE_PARTITION;
        CHS start = CHS.fromByteBuffer(rawPartitionTableEntry.slice(1,3));
        CHS end = CHS.fromByteBuffer(rawPartitionTableEntry.slice(5,3));
        PartitionType partitionType = PartitionType.fromByte(rawPartitionTableEntry.get(4));
        long lbaStart = parseLong(rawPartitionTableEntry.slice(8,4));
        long sectorLength = parseLong(rawPartitionTableEntry.slice(12,4));
        return new PartitionTableEntry(active, start, end, partitionType, lbaStart, sectorLength);
    }

    public static PartitionTableEntry fromByteStream(InputStream byteArrayInputStream) throws IOException {
        boolean active = (byteArrayInputStream.read() & ACTIVE_PARTITION) == ACTIVE_PARTITION;
        CHS start = CHS.fromByteStream(byteArrayInputStream);
        CHS end = CHS.fromByteStream(byteArrayInputStream);
        PartitionType partitionType = PartitionType.fromByte((byte)byteArrayInputStream.read());
        long lbaStart = parseLong(byteArrayInputStream.readNBytes(4));
        long sectorLength = parseLong(byteArrayInputStream.readNBytes(4));
        return new PartitionTableEntry(active, start, end, partitionType, lbaStart, sectorLength);
    }

    public ByteBuffer toBytes() {
        ByteBuffer buffer = ByteBuffer.allocate(PARTITION_TABLE_ENTRY_LENGTH);
        put(buffer);
        return buffer;
    }

    public void put(ByteBuffer buffer) {
        buffer.put(active ? (byte) ACTIVE_PARTITION : 0);
        start.put(buffer);
    }
}


