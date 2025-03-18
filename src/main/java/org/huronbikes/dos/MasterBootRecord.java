package org.huronbikes.dos;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

public class MasterBootRecord extends Sector {
    private final PartitionTableEntry[] partitionTableEntries;

    public PartitionTableEntry getPartitionTableEntry(int partitionNumber) {
        if(partitionNumber < 0 || partitionNumber > 3) {
            throw new IllegalArgumentException("Partition number must be between 0 and 3");
        }
        return partitionTableEntries[partitionNumber];
    }

    public MasterBootRecord(byte[] data) {
        super(data);
        partitionTableEntries = new PartitionTableEntry[4];
        ByteBuffer buffer = ByteBuffer.wrap(data);
        for(int x = 0; x < partitionTableEntries.length; x++) {
            partitionTableEntries[x] = PartitionTableEntry.fromByteBuffer(buffer.slice(446 + (16 * x), 16));
        }
    }

    public MasterBootRecord(InputStream data) throws IOException {
        this(data.readNBytes(Sector.BYTES_PER_SECTOR));
    }

    public ByteBuffer getBootstrapCode() {
        return ByteBuffer.wrap(data, 0, 446);
    }

    public int getBootSignature() {
        int sig = ((int) data[BYTES_PER_SECTOR - 2]) << 8;
        return sig + data[BYTES_PER_SECTOR - 1];
    }

    public void setBootSignature(int signature) {
        data[BYTES_PER_SECTOR - 2] = (byte)((signature & 0x0FF00) >> 8);
        data[BYTES_PER_SECTOR - 1] = (byte)((signature & 0xFF));
    }
}
