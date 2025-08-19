package com.bilibili.cluster.scheduler.api.service.caster;

import com.bilibili.cluster.scheduler.common.dto.caster.resp.BaseComResp;
import com.bilibili.cluster.scheduler.common.dto.clickhouse.clickhouse.ClickhouseDeployDTO;
import com.bilibili.cluster.scheduler.common.dto.presto.template.PrestoDeployDTO;

/**
 * @description:
 * @Date: 2024/8/8 17:34
 * @Author: nizhiqiang
 */
public interface CasterService {

    /**
     * 部署presto，如果preview为true则不执行只返回模版
     *
     * @param prestoDeploy
     * @return
     */
    String deployPresto(PrestoDeployDTO prestoDeploy);

    /**
     * 部署clickhouse，如果preview为true则不执行只返回模版
     * @param clickhouseDeployDTO
     * @return
     */
    void deployClickHouse(ClickhouseDeployDTO clickhouseDeployDTO);

    /**
     * 获取clickhouse在caster最后生成的模版
     * @param clickhouseDeployDTO
     * @return
     */
    String getClickHouseTemplate(ClickhouseDeployDTO clickhouseDeployDTO);

    /**
     * 查询caster日志
     *
     * @param podName
     * @param appId
     * @param env
     * @param cluster
     * @param tail
     * @param container
     * @return
     */
    String queryLog(String podName, String appId, String env, String cluster, Integer tail, String container);

    /**
     * 删除presto
     *
     * @param appId
     * @param env
     * @param clusterName
     */
    BaseComResp deletePresto(String appId, String env, String clusterName);



}
