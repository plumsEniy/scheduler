package com.bilibili.cluster.scheduler.common.enums.clickhouse;

import com.bilibili.cluster.scheduler.common.Constants;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @description:集群类型
 * @Date: 2025/2/8 17:01
 * @Author: nizhiqiang
 */

@AllArgsConstructor
public enum CKClusterType {
    REPLICA(Constants.REPLICA_SHARDS_FILE, Constants.REPLICA_PROPS_FILE),
    ADMIN(Constants.ADMIN_SHARDS_FILE, Constants.ADMIN_PROPS_FILE);

    @Getter
    String shardsFileName;

    @Getter
    String shardsPropsFileName;
}
