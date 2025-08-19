package com.bilibili.cluster.scheduler.common.utils;

import java.time.Instant;

public class TimesUtils {

    public static long generateTimestamp() {
        // 使用java.time.Instant获取当前时间
        Instant now = Instant.now();
        // 将当前时间转换为时间戳
        long timestamp = now.toEpochMilli();
        return timestamp;
    }
}
