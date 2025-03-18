package org.huronbikes.dos.Directory;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

public class FileDateTime {
    private final FileDate fileDate;
    private final FileTime fileTime;

    public FileDateTime(int dateValue) {
        fileDate = FileDate.fromInteger(dateValue);
        fileTime = null;
    }

    public FileDateTime(int dateValue, int timeValue) {
        fileDate = FileDate.fromInteger(dateValue);
        fileTime = FileTime.fromInteger(timeValue);
    }

    public FileDateTime(LocalDate date) {
        fileDate = FileDate.fromLocalDate(date);
        fileTime = null;
    }


    public FileDateTime(LocalDateTime date) {
        fileDate = FileDate.fromLocalDate(date.toLocalDate());
        fileTime = FileTime.fromLocalDateTime(date);
    }

    public int getDateIntegerValue() {
        return fileDate.toInteger();
    }

    public int getTimeIntegerValue() {
        return Optional.ofNullable(fileTime).map(FileTime::toInteger).orElse(0);
    }

    public String toString() {
        if(fileTime == null) {
            return String.format("%02d/%02d/%04d", fileDate.dayOfMonth, fileDate.month, fileDate.year);
        } else {
            return String.format("%02d/%02d/%04d %02d:%02d:%02d", fileDate.month, fileDate.dayOfMonth, fileDate.year, fileTime.hours, fileTime.minutes, fileTime.seconds);
        }
    }

    private record FileDate(int dayOfMonth, int month, int year) {
        private static final int DAY_OF_MONTH_MASK = 0x1F;
        private static final int MONTH_MASK = 0x1E0;
        private static final int YEAR_MASK = 0xFE00;
        private static final int DAY_OF_MONTH_MASK_OFFSET = 0;
        private static final int MONTH_MASK_OFFSET = 5;
        private static final int YEAR_MASK_OFFSET = 9;
        public static final FileDate DEFAULT_FILE_DATE = new FileDate(0,0,0);
        public static final int YEAR_OFFSET = 1980;

        static FileDate fromInteger(int rawFileDate) {
            if(rawFileDate > 0) {
                int dayOfMonth = (rawFileDate & DAY_OF_MONTH_MASK) >> DAY_OF_MONTH_MASK_OFFSET;
                int month = (rawFileDate & MONTH_MASK) >> MONTH_MASK_OFFSET;
                int year = ((rawFileDate & YEAR_MASK) >> YEAR_MASK_OFFSET) + YEAR_OFFSET;
                return new FileDate(dayOfMonth, month, year);
            } else {
                return DEFAULT_FILE_DATE;
            }
        }

        public int toInteger() {
            int result = 0;
            result += (dayOfMonth << DAY_OF_MONTH_MASK_OFFSET) & DAY_OF_MONTH_MASK;
            result += (month << MONTH_MASK_OFFSET) & MONTH_MASK;
            result += (year << YEAR_MASK_OFFSET) & YEAR_MASK;
            return result;
        }

        static FileDate fromLocalDate(LocalDate date) {
            return new FileDate(date.getDayOfMonth(), date.getMonthValue(), date.getYear());
        }
    }

    private record FileTime(int hours, int minutes, int seconds) {
        private static final int HOUR_MASK = 0xF800;
        private static final int HOUR_MASK_OFFSET = 11;
        private static final int MINUTE_MASK = 0x7E0;
        private static final int MINUTE_MASK_OFFSET = 5;
        private static final int SECOND_MASK = 0x1F;
        private static final int SECOND_MASK_OFFSET = 0;

        public static final FileTime DEFAULT_FILE_TIME = new FileTime(0, 0,0);

        static FileTime fromInteger(int rawFileTime) {
            if(rawFileTime > 0) {
                int hours = (rawFileTime & HOUR_MASK) >> HOUR_MASK_OFFSET;
                int minutes = (rawFileTime & MINUTE_MASK) >> MINUTE_MASK_OFFSET;
                int seconds = ((rawFileTime & SECOND_MASK) >> SECOND_MASK_OFFSET) * 2;
                return new FileTime(hours, minutes, seconds);
            } else {
                return DEFAULT_FILE_TIME;
            }
        }

        static FileTime fromLocalDateTime(LocalDateTime time) {
            return new FileTime(time.getHour(), time.getMinute(), time.getSecond());
        }

        int toInteger() {
            int result = 0;
            result += (hours << HOUR_MASK_OFFSET) & HOUR_MASK;
            result += (minutes << MINUTE_MASK_OFFSET) & MINUTE_MASK;
            result += ((seconds / 2) << SECOND_MASK_OFFSET) & SECOND_MASK;
            return  result;
        }
    }

    public static LocalDateTime toLocalDateTime(int fileDate, int fileTime) {
        var date = FileDate.fromInteger(fileDate);
        var time = FileTime.fromInteger(fileTime);
        return LocalDateTime.of(date.year, date.month, date.dayOfMonth, time.hours, time.minutes, time.seconds);
    }

    public static LocalDate toLocalDate(int fileDate) {
        var date = FileDate.fromInteger(fileDate);
        return LocalDate.of(date.year, date.month, date.dayOfMonth);
    }

    public static int toInteger(LocalDate date) {
        if(date == null) {
            return 0;
        } else {
            return FileDate.fromLocalDate(date).toInteger();
        }
    }

    public static int toInteger(LocalDateTime time) {
        if(time == null) {
            return 0;
        } else {
            return FileTime.fromLocalDateTime(time).toInteger();
        }
    }
}
