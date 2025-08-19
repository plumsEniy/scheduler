package com.bilibili.cluster.scheduler.api.service.bmr.resourceV2;

import com.bilibili.cluster.scheduler.common.dto.bmr.resource.req.RefreshNodeListReq;
import com.bilibili.cluster.scheduler.common.dto.bmr.resourceV2.RmsHostInfo;
import com.bilibili.cluster.scheduler.common.dto.bmr.resourceV2.model.ComponentHostRelationModel;
import com.bilibili.cluster.scheduler.common.dto.bmr.resourceV2.model.ResourceHostInfo;
import com.bilibili.cluster.scheduler.common.dto.bmr.resourceV2.model.ResourceLogicGroup;
import com.bilibili.cluster.scheduler.common.dto.bmr.resourceV2.req.QueryComponentHostPageReq;
import com.bilibili.cluster.scheduler.common.dto.bmr.resourceV2.req.QueryLogicGroupInfoReq;
import com.bilibili.cluster.scheduler.common.dto.bmr.resourceV2.TideNodeDetail;
import com.bilibili.cluster.scheduler.common.dto.tide.req.DynamicScalingQueryListPageReq;
import com.bilibili.cluster.scheduler.common.dto.tide.resp.DynamicScalingConfDTO;
import com.bilibili.cluster.scheduler.common.enums.resourceV2.TideClusterType;
import com.bilibili.cluster.scheduler.common.enums.resourceV2.TideNodeStatus;

import java.util.List;

/**
 * @description: bmr的资源管理系统v2
 * @Date: 2024/9/4 16:07
 * @Author: nizhiqiang
 */
public interface BmrResourceV2Service {

    /**
     * @param req 需要集群id和组件id
     * @return
     */
    List<ComponentHostRelationModel> queryComponentHostList(QueryComponentHostPageReq req);

    /**
     * 新资源管理系统刷新节点状态接口
     *
     * @param refreshNodeListReq
     * @return
     */
    Boolean refreshDeployNodeInfo(RefreshNodeListReq refreshNodeListReq);

    /**
     * 根据主机名称查询主机详情信息
     *
     * @param hostList
     * @return
     */
    List<ResourceHostInfo> queryHostInfoByName(List<String> hostList);

    /**
     * 查询逻辑分组列表
     *
     * @param req
     * @return
     */
    List<ResourceLogicGroup> queryLogicGroupList(QueryLogicGroupInfoReq req);

    /**
     * 查询潮汐业务当前使用节点列表
     *
     * @param appId
     * @param tideNodeStatus
     * @return
     */
    List<TideNodeDetail> queryTideOnBizUsedNodes(String appId, TideNodeStatus tideNodeStatus, TideClusterType tideClusterType);


    /**
     * 更新潮汐节点列表服务和状态
     */
    boolean updateTideNodeServiceAndStatus(String hostname, TideNodeStatus nodeStatus, String appId, String deployService, TideClusterType belongResourcePool);

    /**
     * 查询节点信息
     */
    TideNodeDetail queryTideNodeDetail(String hostname);

    /**
     * 查询rms上的主机信息
     */
    RmsHostInfo queryHostRmsInfo(String hostname);


    /**
     * 查询动态扩缩容配置列表
     * @param req
     * @return
     */
    List<DynamicScalingConfDTO> queryDynamicScalingConfList(DynamicScalingQueryListPageReq req);

    /**
     * 查询当前弹性伸缩配置对应的潮汐yarn集群Id
     */
    long queryCurrentYarnTideClusterId(TideClusterType presto);

    /**
     * 判断日期是否为节假日
     */
    boolean isHoliday(String date);

}
