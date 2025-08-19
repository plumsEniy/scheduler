
package com.bilibili.cluster.scheduler.common.utils;

/**
 * General utilities for string-encoding. This class is used to avoid additional dependencies to
 * other projects.
 */
public class EncodingUtils {

    private EncodingUtils() {
        // do not instantiate
    }

    public static String escapeBackticks(String s) {
        return s.replace("`", "``");
    }

    public static String escapeSingleQuotes(String s) {
        return s.replace("'", "''");
    }

    public static String escapeIdentifier(String s) {
        return "`" + escapeBackticks(s) + "`";
    }

}
