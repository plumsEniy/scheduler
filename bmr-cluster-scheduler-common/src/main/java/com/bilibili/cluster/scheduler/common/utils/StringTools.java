package com.bilibili.cluster.scheduler.common.utils;

import com.bilibili.cluster.scheduler.common.Constants;

public class StringTools {

    public static String wrapperString(String s) {
        if (org.apache.commons.lang3.StringUtils.isBlank(s)) {
            return Constants.EMPTY_STRING;
        }
        return s;
    }

}
