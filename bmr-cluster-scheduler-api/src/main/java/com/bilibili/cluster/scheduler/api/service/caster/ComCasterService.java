package com.bilibili.cluster.scheduler.api.service.caster;

import com.bilibili.cluster.scheduler.common.dto.caster.PodInfo;
import com.bilibili.cluster.scheduler.common.dto.caster.PvcInfo;
import com.bilibili.cluster.scheduler.common.dto.caster.ResourceNodeInfo;
import com.bilibili.cluster.scheduler.common.enums.resourceV2.TideClusterType;

import java.util.List;
import java.util.Map;

public interface ComCasterService {

    public String authPlatform(String apiToken, String platformId);

    boolean removeK8sLabel(Integer clusterId, List<String> ipList);

    /**
     * 查询pod信息
     *
     * @param clusterId   必传
     * @param podselector
     * @param hostnames
     * @param namespace
     * @return
     */
    List<PodInfo> queryPodList(long clusterId, String podselector, String hostnames, String namespace);

    /**
     * 根据label map获取pod
     *
     * @param clusterId
     * @param labelMap
     * @param hostnames
     * @param namespace
     * @return
     */
    List<PodInfo> queryPodList(long clusterId, Map<String, Object> labelMap, String hostnames, String namespace);

    /**
     * 查询资源池节点列表信息
     *
     * @param clusterId
     * @return
     */
    List<ResourceNodeInfo> queryAllNodeInfo(long clusterId);

    /**
     * 查询集群中该节点的pod信息
     *
     * @param clusterId
     * @param nodeName  node是ip
     * @return
     */
    List<PodInfo> queryPodListByNodeIp(long clusterId, String nodeName);

    /**
     * 节点开启污点调度
     *
     * @param nodeName (ip)
     * @return
     */
    boolean updateNodeToTaintOn(long clusterId, String nodeName, TideClusterType tideClusterType);

    /**
     * 节点关闭污点调度
     *
     * @param nodeName (ip)
     * @return
     */
    boolean updateNodeToTaintOff(long clusterId, String nodeName, TideClusterType tideClusterType);

    /**
     * 删除pvc节点
     *
     * @param clusterId
     * @param namespace
     * @param pvcNames
     * @return
     */
    void deletePvc(long clusterId, String namespace, List<String> pvcNames);


    /**
     * 查询主机上的pvc
     *
     * @param clusterId
     * @param hostIp
     * @param namespace 可不传
     * @param labelMap  可不传
     * @return
     */
    List<PvcInfo> queryPvcListByHost(long clusterId, String hostIp, String namespace, Map<String, Object> labelMap);
}
