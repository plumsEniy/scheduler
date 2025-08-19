package com.bilibili.cluster.scheduler.common.utils;

import java.util.Objects;

/**
 * @description:
 * @Date: 2024/1/19 14:20
 * @Author: nizhiqiang
 */
public class NumberUtils {

    public static boolean isPositiveLong(Long target) {
        if (Objects.isNull(target)) {
            return false;
        }

        if (target.longValue() <= 0) {
            return false;
        }
        return true;
    }


    public static boolean isPositiveInteger(Integer target) {
        if (Objects.isNull(target)) {
            return false;
        }

        if (target.intValue() <= 0) {
            return false;
        }
        return true;
    }
}
