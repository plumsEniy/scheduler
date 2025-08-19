package com.bilibili.cluster.scheduler.common.utils;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;

public class DateTimeUtils {

    /**
     * LocalDateTime转毫秒时间戳
     * @param localDateTime LocalDateTime
     * @return 时间戳
     */
    public static Long localDateTimeToTimestamp(LocalDateTime localDateTime) {
        ZoneId zoneId = ZoneId.systemDefault();
        Instant instant = localDateTime.atZone(zoneId).toInstant();
        return instant.toEpochMilli();
    }

    /**
     * 时间戳转LocalDateTime
     * @param timestamp 时间戳
     * @return LocalDateTime
     */
    public static LocalDateTime timestampToLocalDateTime(long timestamp) {
        Instant instant = Instant.ofEpochMilli(timestamp);
        ZoneId zone = ZoneId.systemDefault();
        return LocalDateTime.ofInstant(instant, zone);
    }

    /**
     * Date转LocalDateTime
     * @param date Date
     * @return LocalDateTime
     */
    public static LocalDateTime dateToLocalDateTime(Date date) {
        Instant instant = date.toInstant();
        ZoneId zoneId = ZoneId.systemDefault();
        return instant.atZone(zoneId).toLocalDateTime();
    }

    /**
     * LocalDateTime转Date
     * @param localDateTime LocalDateTime
     * @return Date
     */
    public static Date localDateTimeToDate(LocalDateTime localDateTime) {
        ZoneId zoneId = ZoneId.systemDefault();
        ZonedDateTime zdt = localDateTime.atZone(zoneId);
        return Date.from(zdt.toInstant());
    }

    public static String formatTimeStamp(long ts, String format) {
        LocalDateTime dateTime = timestampToLocalDateTime(ts);
        return LocalDateFormatterUtils.format(format, dateTime);
    }

}
