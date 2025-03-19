package org.huronbikes.dos;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class Sector {

    @FunctionalInterface
    public interface CreateFromStream<T extends InputStream, U extends Sector> {
        U apply(T inputStream) throws IOException;
    }

    public static final int BYTES_PER_SECTOR = 512;
    protected final byte[] data;

    protected Sector(byte[] buffer) {
        data = Arrays.copyOf(buffer, BYTES_PER_SECTOR);
    }

    public Sector(InputStream stream) throws IOException {
        this(stream.readNBytes(BYTES_PER_SECTOR));
    }

    public static <T extends Sector> T atPosition(InputStream stream, int sectorNumber, CreateFromStream<InputStream, T> creator) throws IOException {
        long sectorStart = sectorNumber * (long) BYTES_PER_SECTOR;
        if (stream.skip(sectorStart) != sectorStart) {
            throw new IllegalArgumentException(String.format("Sector %d (%d bytes offset) is not available in the provided stream", sectorNumber, sectorStart));
        }
        T result = creator.apply(stream);
        return result;
    }

    public static Sector atPosition(InputStream stream, int sectorNumber) throws IOException {
        long sectorStart = sectorNumber * (long) BYTES_PER_SECTOR;
        if (stream.skip(sectorStart) != sectorStart) {
            throw new IllegalArgumentException(String.format("Sector %d (%d bytes offset) is not available in the provided stream", sectorNumber, sectorStart));
        }
        Sector result = new Sector(stream);
        return result;
    }

    protected void writeToData(byte[] source, int offset, int length) {
        if (source.length < length) {
            throw new IllegalArgumentException("source data must equal or exceed write length");
        }

        if (offset > data.length) {
            throw new IllegalArgumentException("offset must be within the bounds of the underlying data");
        }

        if (offset + length > data.length) {
            throw new IllegalArgumentException("write operation would exceed the bounds of the data area");
        }

        if (length >= 0) System.arraycopy(source, 0, data, offset, length);
    }


    public void textOutput() {
        var line = new byte[64];
        for(int offset = 0; offset < data.length; offset+= line.length) {
            for (int i = 0; i + offset < data.length && i < line.length; i++) {
                line[i] = data[offset + i] >= 48 && data[offset + i] < 127 ? data[offset + i] : 32;
            }
            System.out.println(new String(line));
        }
    }
}
