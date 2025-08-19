package com.bilibili.cluster.scheduler.api.service.bmr.spark.ess;

import java.util.List;

public interface SparkEssMasterService {

    /**
     * 添加SparkEssMaster黑名单
     */
    boolean addBlackList(List<String> hostList, String sparkEssMasterHostName);

    /**
     * 移除SparkEssMaster黑名单
     */
    boolean removeBlackList(List<String> hostList, String sparkEssMasterHostName);

}
