package org.huronbikes.dos;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

@Data
@AllArgsConstructor
public class CHS {
    private int cylinder;
    private int sector;
    private int head;

    public static CHS fromBytes(byte[] chsAsBytes) {
        return fromByteBuffer(ByteBuffer.wrap(chsAsBytes));
    }

    public static CHS fromByteStream(InputStream stream) throws IOException {
        return fromBytes(stream.readNBytes(3));
    }

    public static CHS fromByteBuffer(ByteBuffer buffer) {
        int head= buffer.get(0);
        int sector=((int)buffer.get(1)) & 0x03f;
        int cylinder = (((int) buffer.get(1)) & 0x0C0) << 2;
        cylinder += buffer.get(2);
        return new CHS(cylinder, sector, head);
    }

    public void put(ByteBuffer buffer) {
        byte sectorAndCylinderHigh = (byte)(((cylinder & 0x300) >> 2) + sector);
        buffer.put((byte) head);
        buffer.put(sectorAndCylinderHigh);
        buffer.put((byte) (0xFF & cylinder));
    }

    public ByteBuffer toBytes() {
        ByteBuffer buffer = ByteBuffer.allocate(3);
        put(buffer);
        return buffer;
    }
}
