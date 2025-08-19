package com.bilibili.cluster.scheduler.api.service.clickhouse.clickhouse;

import com.bilibili.cluster.scheduler.common.dto.clickhouse.clickhouse.ClickhouseDeployDTO;
import com.bilibili.cluster.scheduler.common.dto.clickhouse.clickhouse.PodTemplateDTO;
import com.bilibili.cluster.scheduler.common.dto.clickhouse.clickhouse.ShardAllocationDTO;
import com.bilibili.cluster.scheduler.common.dto.clickhouse.clickhouse.templates.ClickhouseCluster;
import com.bilibili.cluster.scheduler.common.dto.clickhouse.clickhouse.templates.PaasConfig;
import com.bilibili.cluster.scheduler.common.enums.clickhouse.CKClusterType;

import java.util.List;

/**
 * @description:
 * @Date: 2025/1/24 14:47
 * @Author: nizhiqiang
 */
public interface ClickhouseService {

    /**
     * 根据配置版本ID和podTemplate创建ck发送给caster的对象，并根据podTemplate替换容器模版
     *
     * @param configVersionId 配置版本ID，用于构建Clickhouse部署DTO
     * @param podTemplate     自定义的podTemplate，如果提供，则替换默认的podTemplate
     * @return 返回更新后的Clickhouse部署DTO的JSON字符串表示
     * <p>
     * 此方法首先构建一个ClickhouseDeployDTO对象，然后根据是否提供了podTemplate，
     * 查找并替换所有集群配置中的podTemplate如果提供的podTemplate不存在于配置中，
     * 则抛出运行时异常
     */
    ClickhouseDeployDTO buildClickhouseDeployDTO(long configVersionId);

    /**
     * 查询ck的pod模版列表（对应podtemplates.yaml文件）
     *
     * @param configVersionId
     * @return
     */
    List<PodTemplateDTO> queryPodTemplateList(long configVersionId);

    /**
     * 构建扩缩容后的ClickhouseDeployDTO
     *
     * @param configVersionId
     * @param podTemplate         扩容容器使用的模版
     * @param shardAllocationList 扩容的shards组划分[
     * @return
     */
    ClickhouseDeployDTO buildScaleDeployDTO(long configVersionId, String podTemplate, List<Integer> shardAllocationList);

    /**
     * 构建迭代后的ClickhouseDeployDTO
     *
     * @param configVersionId
     * @param podTemplate
     * @param nodeList
     * @return
     */
    ClickhouseDeployDTO buildIterationDeployDTO(long configVersionId, String podTemplate, List<String> nodeList);

    /**
     * 构建paasconfig
     *
     * @param configVersionId
     * @return
     */
    PaasConfig getPaasConfig(long configVersionId);

    /**
     * 查询shard列表
     *
     * @param configVersionId
     * @return
     */
    ShardAllocationDTO queryShardList(long configVersionId);

    /**
     * 获取pod链接
     *
     * @param configVersionId
     * @param podName
     * @return
     */
    String getPodUrl(long clusterId, long configVersionId, String podName);

    /**
     * 查询pod日志
     *
     * @param configVersionId
     * @param podName
     * @param limit
     * @return
     */
    String queryPodLog(long clusterId, long configVersionId, String podName, int limit);

    /**
     * 更新shard文件，包含 admin和replica文件
     *
     * @param componentId
     * @param clusterList
     */
    void updateShardFile(Long componentId, List<ClickhouseCluster> clusterList);

    /**
     * 根据配置版本ID构建ck集群对象（主要是shard和pod实例分配）
     *
     * @return
     */
    ClickhouseCluster buildCkCluster(long configVersionId, CKClusterType clusterType);

    /**
     * 根据集群ID查询pod模版列表
     *
     * @param clusterId
     * @return
     */
    List<PodTemplateDTO> queryPodTemplateListByClusterId(long clusterId);
}
