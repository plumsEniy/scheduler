package com.bilibili.cluster.scheduler.common.enums.resourceV2;

import com.bilibili.cluster.scheduler.common.Constants;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @description: 潮汐集群类型
 * @Date: 2025/3/19 16:30
 * @Author: nizhiqiang
 */

@AllArgsConstructor
public enum TideClusterType {

    PRESTO(Constants.PRESTO_TAINT_KEY, Constants.PRESTO_TAINT_VALUE),
    CLICKHOUSE(Constants.CK_TAINT_KEY, Constants.CK_TAINT_VALUE);

    /**
     * 污点的key
     */
    @Getter
    String taintKey;

    /**
     * 污点的value
     */
    @Getter
    String taintValue;


}
