package com.bilibili.cluster.scheduler.common.utils;

import com.bilibili.cluster.scheduler.common.Constants;

public class EnvUtils {

    public static boolean isProd(String env) {
        if (Constants.PROD_ENV.equalsIgnoreCase(env)) return true;
        else return false;
    }

    public static boolean isPre(String env) {
        if (Constants.PRE_ENV.equalsIgnoreCase(env)) return true;
        else return false;
    }

    public static boolean isUat(String env) {
        if (Constants.UAT_ENV.equalsIgnoreCase(env)) return true;
        else return false;
    }
}
