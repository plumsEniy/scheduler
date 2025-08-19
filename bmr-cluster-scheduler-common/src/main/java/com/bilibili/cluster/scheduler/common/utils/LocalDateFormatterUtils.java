package com.bilibili.cluster.scheduler.common.utils;

import com.bilibili.cluster.scheduler.common.Constants;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * @description:
 * @Date: 2024/1/19 14:47
 * @Author: nizhiqiang
 */
public class LocalDateFormatterUtils {

    private static Map<String, DateTimeFormatter> formatterMap = new HashMap<>();

    static {
        formatterMap.put(Constants.FMT_MONTH, DateTimeFormatter.ofPattern(Constants.FMT_MONTH).withZone(ZoneId.systemDefault()));
        formatterMap.put(Constants.FMT_DAY, DateTimeFormatter.ofPattern(Constants.FMT_DAY).withZone(ZoneId.systemDefault()));
        formatterMap.put(Constants.FMT_DATE_TIME, DateTimeFormatter.ofPattern(Constants.FMT_DATE_TIME).withZone(ZoneId.systemDefault()));
        formatterMap.put(Constants.FMT_DATE_TIME_SECOND, DateTimeFormatter.ofPattern(Constants.FMT_DATE_TIME_SECOND).withZone(ZoneId.systemDefault()));

        formatterMap.put(Constants.FMT_HOUR, DateTimeFormatter.ofPattern(Constants.FMT_HOUR).withZone(ZoneId.systemDefault()));
        formatterMap.put(Constants.FMT_MILLS, DateTimeFormatter.ofPattern(Constants.FMT_MILLS).withZone(ZoneId.systemDefault()));
        formatterMap.put(Constants.FMT_MINS, DateTimeFormatter.ofPattern(Constants.FMT_MINS).withZone(ZoneId.systemDefault()));
        formatterMap.put(Constants.FMT_DATE_TIME_UNIT, DateTimeFormatter.ofPattern(Constants.FMT_DATE_TIME_UNIT).withZone(ZoneId.systemDefault()));
        formatterMap.put(Constants.FMT_DATE_TIME_UNIT_SECOND, DateTimeFormatter.ofPattern(Constants.FMT_DATE_TIME_UNIT_SECOND).withZone(ZoneId.systemDefault()));
    }

    public static LocalDateTime parseByPattern(String pattern, String date) {
        return LocalDateTime.parse(date,
                Optional.ofNullable(formatterMap.get(pattern)).orElseThrow(RuntimeException::new));
    }

    public static LocalDate parseDateByPattern(String pattern, String date) {
        return LocalDate.parse(date,
                Optional.ofNullable(formatterMap.get(pattern)).orElseThrow(RuntimeException::new));
    }

    public static String format(String pattern, LocalDateTime dateTime) {
        return Optional.ofNullable(formatterMap.get(pattern)).orElseThrow(RuntimeException::new).format(dateTime);
    }

    public static String formatDate(String pattern, LocalDate date) {
        return Optional.ofNullable(formatterMap.get(pattern)).orElseThrow(RuntimeException::new).format(date);
    }

    public static LocalDateTime approximateHours(LocalDateTime dateTime) {
        String format = format(Constants.FMT_HOUR, dateTime);
        return parseByPattern(Constants.FMT_HOUR, format);
    }

    public static String getNextDay(String onCallDate) {
        LocalDate date = parseDateByPattern(Constants.FMT_DAY, onCallDate);
        return formatDate(Constants.FMT_DAY, date.plusDays(1));
    }

    public static Date asDate(LocalDate localDate) {
        return Date.from(localDate.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant());
    }

    public static Date asDate(LocalDateTime localDateTime) {
        return Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
    }

    public static LocalDate asLocalDate(Date date) {
        return Instant.ofEpochMilli(date.getTime()).atZone(ZoneId.systemDefault()).toLocalDate();
    }

    public static LocalDateTime asLocalDateTime(Date date) {
        return Instant.ofEpochMilli(date.getTime()).atZone(ZoneId.systemDefault()).toLocalDateTime();
    }

    public static String getNowDefaultFmt() {
        return format(Constants.FMT_DATE_TIME, LocalDateTime.now());
    }

    public static String getNowMilliFmt() {
        return format(Constants.FMT_MILLS, LocalDateTime.now());
    }
}
