package com.bilibili.cluster.scheduler.api.service.bmr.resource;

import com.bilibili.cluster.scheduler.common.dto.bmr.resource.ComponentNodeDetail;
import com.bilibili.cluster.scheduler.common.dto.bmr.resource.model.HostAndLogicGroupInfo;
import com.bilibili.cluster.scheduler.common.dto.bmr.resource.model.ResourceHostInfo;
import com.bilibili.cluster.scheduler.common.dto.bmr.resource.req.QueryComponentNodeListReq;
import com.bilibili.cluster.scheduler.common.dto.yarn.RMInfoObj;
import com.bilibili.cluster.scheduler.common.enums.flow.FlowDeployType;
import com.bilibili.cluster.scheduler.common.response.BaseMsgResp;

import java.util.List;
import java.util.Map;

/**
 * @description: bmr的资源管理系统接口
 * @Date: 2024/3/11 20:01
 * @Author: nizhiqiang
 */
public interface BmrResourceService {


    /**
     * 变更yarn的label，目前只使用生成的bmr接口
     *
     * @param hostNameList
     * @param label
     * @return
     */
    boolean alterYarnLabel(List<String> hostNameList, String label);

    /**
     * 查询namenode节点
     *
     * @param clusterId
     * @return
     */
    List<ComponentNodeDetail> queryNameNodeHostByClusterId(long clusterId);

    /**
     * 查询节点组信息
     *
     * @param clusterId
     * @param nodeList
     * @return
     */
    Map<String, HostAndLogicGroupInfo> queryNodeGroupInfo(long clusterId, List<String> nodeList);

    /**
     * 过滤出jobagent异常的列表
     *
     * @param nodeList
     * @return
     */
    List<String> filterJobAgentLiveness(List<String> nodeList);

    /**
     * 根据集群id查询所有的namenode的安全模式
     *
     * @param clusterId
     * @return
     */
    BaseMsgResp checkNameNodeSafeModeByClusterId(long clusterId);

    /**
     * 根据主机名查询任务详情
     *
     * @param hostnameList
     * @return
     */
    List<ResourceHostInfo> queryHostListByName(List<String> hostnameList);

    /**
     * 更新节点状态
     *
     * @param clusterId
     * @param componentId
     * @param nodeList
     * @param deployType
     * @param success
     * @param packageVersion
     * @param configVersion
     * @return
     */
    Boolean updateNodeListState(long clusterId, long componentId, List<String> nodeList, FlowDeployType deployType, boolean success,
                                String packageVersion, String configVersion);


    /**
     * 查询集群下resourceManager信息
     * @param yarnClusterId
     * @return
     */
    RMInfoObj queryRMComponentIdByClusterId(long yarnClusterId);

    /**
     * 根据clusterId和componentId查询节点列表
     * @param clusterId
     * @param componentId
     * @return
     */
    List<ComponentNodeDetail> queryComponentNodeList(long clusterId, long componentId);

    /**
     * 添加主机到指定节点分组
     * @param clusterId
     * @param hostList
     * @param nodeGroupName
     * @return
     */
    List<String> addHostToTideNodeGroup(long clusterId, List<String> hostList, String nodeGroupName);

    /**
     * 切换yarn集群nodeLabel
     * @param clusterId
     * @param hostname
     * @param nodeLabel
     * @return
     */
    boolean switchYarnNodeLabel(long clusterId, String hostname, String nodeLabel);

    /**
     * 查询节点列表
     * @param req
     * @return
     */
    List<ComponentNodeDetail> queryNodeList(QueryComponentNodeListReq req);

    /**
     * 查询zk角色
     * @param hostName
     * @return
     */
    String queryZkRole(String hostName);

}
