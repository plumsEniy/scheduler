package com.bilibili.cluster.scheduler.api.service.presto;

import com.bilibili.cluster.scheduler.common.dto.presto.PrestoNodeInfo;
import com.bilibili.cluster.scheduler.common.dto.presto.PrestoYamlObj;
import com.bilibili.cluster.scheduler.common.dto.presto.template.PrestoDeployDTO;
import com.bilibili.cluster.scheduler.common.response.ResponseResult;

/**
 * @description: presto的服务
 * @Date: 2024/6/7 14:34
 * @Author: nizhiqiang
 */
public interface PrestoService {
    /**
     * 查询presto模版
     *
     * @return
     */
    String queryPrestoTemplate(long clusterId, long configVersionId, String image);

    /**
     * 部署presto
     *
     * @param clusterId
     * @param configVersionId
     * @param image
     * @return
     */
    void deployPresto(long clusterId, long configVersionId, String image);

    /**
     * 获取部署presto的模版
     *
     * @param clusterId
     * @param configVersionId
     * @param image
     */
    String getDeployPrestoTemplate(long clusterId, long configVersionId, String image);

    /**
     * 启用集群
     *
     * @param clusterName
     */
    void activeCluster(String clusterName, String env);

    /**
     * 停止集群
     *
     * @param clusterName
     */
    void deactivateCluster(String clusterName, String env);

    /**
     * 获取presto部署的req
     *
     * @param configVersionId
     * @param image
     * @return
     */
    PrestoDeployDTO generateDeployPrestoReq(long clusterId, long configVersionId, String image);

    /**
     * 根据模版查询节点数量
     *
     * @param clusterId
     * @param configVersionId
     * @return
     */
    PrestoNodeInfo queryNodesByTemplate(long clusterId, long configVersionId);

    /**
     * 构建模版对象
     *
     * @param configVersionId
     * @param image
     * @return
     */
    PrestoYamlObj buildPrestoYamlObj(long configVersionId, String image);

    /**
     * 查询pod日志
     *
     * @param clusterId
     * @param podName
     * @return
     */
    String queryPodLog(long clusterId, String podName, int limit);

    /**
     * 根据组件查询最新的节点数量
     *
     * @param clusterId
     * @param componentId
     * @return
     */
    PrestoNodeInfo queryNodesByComponentId(long clusterId, long componentId);

    /**
     * 取消presto节点污点配置（caster and ）
     * @param hostname
     * @return
     */
    ResponseResult cancelNodeTaintStatus(String hostname);

    /**
     * 新增presto污点配置
     * @param hostname
     * @param appId
     * @return
     */
    ResponseResult addNodeTaintStatus(String hostname, String appId);

    /**
     * caster资源池集群id
     * @return
     */
    long getPrestoCasterClusterId();
}
