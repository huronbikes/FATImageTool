package org.huronbikes.dos;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class ByteUtils {
    private static final String BLANK_EXTENSION = new String(new byte[] {32, 32, 32}, StandardCharsets.US_ASCII);

    public static long parseLong(ByteBuffer longValue) {
        long result = 0;
        for(int x = longValue.limit() - 1; x >= longValue.position(); x--) {
            result = result << 8;
            result += 0x0FF & longValue.get(x);
        }
        return result;
    }

    public static long parseLong(byte[] longValue) {
        return parseLong(longValue, 0, longValue.length);
    }

    public static long parseLong(byte[] longValue, int offset, int length) {
        long result = 0;
        for(int x = 0; x + offset < longValue.length && x < length; x++) {
            result += ((long)(0x0FF & longValue[x + offset]) << (x * 8));
        }
        return result;
    }

    public static void writeWord(byte[] target, int value, int offset) {
        target[offset] = (byte)(0x0FF & value);
        target[offset + 1] = (byte)((0xFF00 & value) >> 8);
    }

    public static void writeDWord(byte[] target, long value, int offset) {
        target[offset] = (byte)(0x0FF & value);
        target[offset + 1] = (byte)((value >> 8) & 0xFF);
        target[offset + 2] = (byte)((value >> 16) & 0xFF);
        target[offset + 3] = (byte)((value >> 24) & 0xFF);
    }

    public static long parseLong(ByteBuffer source, int offset, int length) {
        byte[] buffer = new byte[length];
        source.position(offset);
        source.get(buffer);
        return parseLong(buffer);
    }

    public static String asciiString(byte[] bytes, int offset, int length) {
        return StandardCharsets.US_ASCII.decode(ByteBuffer.wrap(bytes, offset, length)).toString();
    }

    public static byte[] shortName(String name) {
        return shortName(name, BLANK_EXTENSION);
    }

    public static byte[] shortName(String name, String extension) {
        var shortName = new byte[11];
        var nameArray = name.getBytes(StandardCharsets.US_ASCII);
        var extensionArray = extension.getBytes(StandardCharsets.US_ASCII);
        Arrays.fill(shortName, (byte)32);
        for(int i = 0; i < 8 && i < name.length(); i++) {
            shortName[i] = nameArray[i];
        }
        for(int i = 8; i < 11 && i < 8 +extension.length(); i++) {
            shortName[i] = extensionArray[i - 8];
        }
        return shortName;
    }
}
