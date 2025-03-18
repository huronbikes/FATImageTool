package org.huronbikes.dos;

import lombok.Getter;

import java.util.Arrays;

public class MediaType {
    public interface Indicator {
        int getValue();
        DiskType getDiskType();
    }

    public enum Floppy525 implements Indicator{
        /**
         * For 5.25" floppies:
         * Value  DOS version  Capacity  sides  tracks  sectors/track
         * ff     1.1           320 KB    2      40      8
         * fe     1.0           160 KB    1      40      8
         * fd     2.0           360 KB    2      40      9
         * fc     2.0           180 KB    1      40      9
         * fb                   640 KB    2      80      8
         * fa                   320 KB    1      80      8
         * f9     3.0          1200 KB    2      80     15
         */

        FLOPPY_DS_320(0xFF),
        FLOPPY_160(0xFE),
        FLOPPY_360(0xFD),
        FLOPPY_180(0xFC),
        FLOPPY_640(0xFB),
        FLOPPY_DD_320(0xFA),
        FLOPPY_1200(0xF9);

        @Getter
        private int value;

        public DiskType getDiskType() {
            return DiskType.FLOPPY_525;
        }

        Floppy525(int val) {
            value = val;
        }
    }

    public enum Floppy35 implements Indicator{
        /**
         * For 3.5" floppies:
         * Value  DOS version  Capacity  sides  tracks  sectors/track
         * fb                   640 KB    2      80      8
         * fa                   320 KB    1      80      8
         * f9     3.2           720 KB    2      80      9
         * f0     3.3          1440 KB    2      80     18
         * f0                  2880 KB    2      80     36
         */
        FLOPPY_640(0xFB),
        FLOPPY_320(0xFA),
        FLOPPY_720(0xF9),
        FLOPPY_1440_2880(0xF0);

        @Getter
        private int value;

        public DiskType getDiskType() {
            return DiskType.FLOPPY_35;
        }

        Floppy35(int val) {
            value = val;
        }
    }


    public enum FixedDisk implements Indicator {
        ANY(0xF8);

        @Getter
        private int value;

        public DiskType getDiskType() {
            return DiskType.FIXED;
        }

        FixedDisk(int val) {
            value = val;
        }
    }

    public enum DiskType {
        FLOPPY_525,
        FLOPPY_35,
        FIXED,
        UNKNOWN
    }

    public enum Unknown implements Indicator {
        UNKNOWN;

        public int getValue() {
            return 0;
        }

        public DiskType getDiskType() {
            return DiskType.UNKNOWN;
        }
    }

    public static Indicator getIndicator(int indicator, DiskType diskType) {
        return switch(diskType) {
            case FIXED -> indicator == FixedDisk.ANY.value ? FixedDisk.ANY : Unknown.UNKNOWN;
            case FLOPPY_35 -> Arrays.stream(Floppy35.values()).filter(v -> v.value == indicator).findFirst().map(i -> (Indicator) i).orElse(Unknown.UNKNOWN);
            case FLOPPY_525 -> Arrays.stream(Floppy525.values()).filter(v -> v.value == indicator).findFirst().map(i -> (Indicator) i).orElse(Unknown.UNKNOWN);
            case UNKNOWN -> Unknown.UNKNOWN;
        };
    }
}
