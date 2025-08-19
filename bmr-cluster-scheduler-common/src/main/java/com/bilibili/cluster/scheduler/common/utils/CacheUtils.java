package com.bilibili.cluster.scheduler.common.utils;

import com.bilibili.cluster.scheduler.common.Constants;

public class CacheUtils {

    public static String getFlowExtParamsCacheKey(String env, long flowId) {
        String cacheKey = new StringBuilder()
                .append(Constants.SCHEDULER).append(Constants.UNDER_LINE)
                .append(env).append(Constants.UNDER_LINE)
                .append(Constants.FLOW_EXT_PARAMS).append(Constants.UNDER_LINE)
                .append(flowId).toString();
        return cacheKey;
    }

    public static String getFlowPropsCacheKey(String env, long flowId) {
        String cacheKey = new StringBuilder()
                .append(Constants.SCHEDULER).append(Constants.UNDER_LINE)
                .append(env).append(Constants.UNDER_LINE)
                .append(Constants.FLOW_PROPS_PARAMS).append(Constants.UNDER_LINE)
                .append(flowId).toString();
        return cacheKey;
    }

    public static String getFlowMaxStageCacheKey(String env, long flowId) {
        String cacheKey = new StringBuilder()
                .append(Constants.SCHEDULER).append(Constants.UNDER_LINE)
                .append(env).append(Constants.UNDER_LINE)
                .append(Constants.FLOW_MAX_STAGE_KEY).append(Constants.UNDER_LINE)
                .append(flowId).toString();
        return cacheKey;
    }

    public static String getMaxNodeIdByStageCacheKey(String env, long flowId, String stage) {
        String cacheKey = new StringBuilder()
                .append(Constants.SCHEDULER).append(Constants.UNDER_LINE)
                .append(env).append(Constants.UNDER_LINE)
                .append(Constants.MAX_NODE_ID_BY_STAGE_KEY).append(Constants.UNDER_LINE)
                .append(flowId).append(Constants.UNDER_LINE)
                .append(stage).append(Constants.UNDER_LINE)
                .toString();
        return cacheKey;
    }


    public static String getFlowMinStageCacheKey(String env, Long flowId) {
        String cacheKey = new StringBuilder()
                .append(Constants.SCHEDULER).append(Constants.UNDER_LINE)
                .append(env).append(Constants.UNDER_LINE)
                .append(Constants.FLOW_MIN_STAGE_KEY).append(Constants.UNDER_LINE)
                .append(flowId).toString();
        return cacheKey;
    }
}
