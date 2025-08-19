package com.bilibili.cluster.scheduler.api.event.clickhouse.tide;

import cn.hutool.json.JSONUtil;
import com.bilibili.cluster.scheduler.api.service.caster.ComCasterService;
import com.bilibili.cluster.scheduler.api.service.flow.ExecutionFlowPropsService;
import com.bilibili.cluster.scheduler.common.Constants;
import com.bilibili.cluster.scheduler.common.dto.caster.PodInfo;
import com.bilibili.cluster.scheduler.common.dto.caster.ResourceNodeInfo;
import com.bilibili.cluster.scheduler.common.dto.clickhouse.clickhouse.params.CkTideExtFlowParams;
import com.bilibili.cluster.scheduler.common.dto.clickhouse.clickhouse.templates.ClickhouseCluster;
import com.bilibili.cluster.scheduler.common.dto.clickhouse.clickhouse.templates.Replica;
import com.bilibili.cluster.scheduler.common.dto.clickhouse.clickhouse.templates.Shards;
import com.bilibili.cluster.scheduler.common.dto.flow.prop.BaseFlowExtPropDTO;
import com.bilibili.cluster.scheduler.common.entity.ExecutionNodeEntity;
import com.bilibili.cluster.scheduler.common.enums.clickhouse.CKClusterType;
import com.bilibili.cluster.scheduler.common.enums.event.EventTypeEnum;
import com.bilibili.cluster.scheduler.common.event.TaskEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @description:
 * @Date: 2024/12/3 15:24
 * @Author: nizhiqiang
 */

@Slf4j
@Component
public class CkTideOffPodEventHandler extends AbstractCkTideDeployEventHandler {

    @Resource
    ComCasterService comCasterService;

    @Resource
    ExecutionFlowPropsService executionFlowPropsService;

    private static Pattern indexPattern = Pattern.compile("(\\d{2})-0$");

    @Override
    public EventTypeEnum getHandleEventType() {
        return EventTypeEnum.CK_TIDE_OFF_POD_FAST_SHRINKAGE;
    }

    private Integer getPodIndex(String podName) {
        Matcher matcher = indexPattern.matcher(podName);
        if (matcher.find()) {
            int index = Integer.parseInt(matcher.group(1));
            return index;
        }
        return -1;
    }

    /**
     * ck节点快速缩容,仅在阶段一执行
     *
     * @param taskEvent
     * @return
     */
    @Override
    protected boolean checkEventIsRequired(TaskEvent taskEvent) {
        final ExecutionNodeEntity executionNode = taskEvent.getExecutionNode();
        final String execStage = executionNode.getExecStage();
        if (execStage.equalsIgnoreCase("1")) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    List<Integer> getShardAllocationList(TaskEvent taskEvent, CkTideExtFlowParams ckTideExtFlowParams, Long configVersionId) {
        Map<String, Object> labelMap = new HashMap<>();

        List<ResourceNodeInfo> ckResourceNodeList = comCasterService.queryAllNodeInfo(Constants.CK_K8S_CLUSTER_ID);
//        可调度的主机名列表
        Set<String> scheduleNodeList = ckResourceNodeList.stream()
                .filter(nodeInfo -> !nodeInfo.isUnSchedulable())
                .filter(nodeInfo -> Constants.CK_ON_ICEBERG_POOL_NAME.equalsIgnoreCase(nodeInfo.getLabels().get("pool")))
                .map(ResourceNodeInfo::getHostname)
                .collect(Collectors.toSet());

        String appId = ckTideExtFlowParams.getAppId();

        labelMap.put("app_id", appId);
        labelMap.put("scene", "clickhouse");
        labelMap.put("antitide-delete", true);

        ClickhouseCluster adminCkCluster = clickhouseService.buildCkCluster(configVersionId, CKClusterType.ADMIN);
        List<Shards> adminShardList = adminCkCluster.getLayout().getShards();

        List<PodInfo> antiLabelPodList = comCasterService.queryPodList(Constants.CK_K8S_CLUSTER_ID, labelMap, null, null);
        List<List<PodInfo>> sortedPodList = antiLabelPodList.stream()
                .filter(podInfo -> scheduleNodeList.contains(podInfo.getHostname()))
                .collect(Collectors.groupingBy(PodInfo::getHostname))
                .entrySet()
                .stream()
                .sorted(Comparator.comparingInt(entry -> entry.getValue().size()))
                .map(Map.Entry::getValue)
                .collect(Collectors.toList());
//        防止pod已经缩容后失败重试导致的pod被清空
        int evictionPodSize = Math.max(Math.min(ckTideExtFlowParams.getCurrentPod(), adminShardList.size()) - ckTideExtFlowParams.getRemainPod(), 0);

        List<PodInfo> needRemovePodList = new LinkedList<>();
//        贪心选择pod，先从主机上pod最少的pod移除，再从主机上pod数量最多的pod移除
        for (List<PodInfo> podInfoList : sortedPodList) {
            int currentPodListSize = podInfoList.size();

            if (needRemovePodList.size() + currentPodListSize >= evictionPodSize) {
                needRemovePodList.addAll(podInfoList.subList(0, evictionPodSize - needRemovePodList.size()));
                break;
            }
            needRemovePodList.addAll(podInfoList);
        }


        Set<Integer> needRemovePodIndexSet = needRemovePodList.stream()
                .map(PodInfo::getName)
                .map(this::getPodIndex)
                .filter(index -> index > 0)
                .collect(Collectors.toSet());
        logPersist(taskEvent, String.format("needRemovePodIndexSet:%s", needRemovePodIndexSet));

        List<Integer> replicaIndexList = buildReplicaIndexList(needRemovePodIndexSet, adminShardList);
        logPersist(taskEvent, String.format("build pod set is :%s", replicaIndexList));

        List<Integer> shardAllocationList = buildShardAllocationList(replicaIndexList);

        if (needRemovePodIndexSet.size() > 0) {
            ckTideExtFlowParams.setActualTidePodCount(needRemovePodIndexSet.size());
        }

        ckTideExtFlowParams.setActualRemainPodCount(replicaIndexList.size());
        Map<String, Set<String>> hostToRemovePodMap = ckTideExtFlowParams.getHostIpToRemovePodMap();
        for (PodInfo removePodInfo : needRemovePodList) {
            String hostIp = removePodInfo.getHostIp();
            Set<String> removePodNameList = hostToRemovePodMap.getOrDefault(hostIp, new HashSet<>());
            removePodNameList.add(removePodInfo.getName());
            hostToRemovePodMap.put(hostIp, removePodNameList);
        }
        ckTideExtFlowParams.setHostIpToRemovePodMap(hostToRemovePodMap);

//        保持并更新ck的参数
        Long flowId = taskEvent.getFlowId();
        BaseFlowExtPropDTO baseFlowExtPropDTO = executionFlowPropsService.getFlowPropByFlowId(flowId, BaseFlowExtPropDTO.class);
        baseFlowExtPropDTO.setFlowExtParams(JSONUtil.toJsonStr(ckTideExtFlowParams));
        executionFlowPropsService.saveFlowProp(flowId, baseFlowExtPropDTO);
        return shardAllocationList;
    }

    /**
     * 根据给定的副本索引列表构建分片分配列表。
     * <p>
     * 该函数通过遍历副本索引列表，生成一个表示分片分配的链表。链表中每个元素为0或1，
     * 其中1表示该位置有副本分配，0表示该位置没有副本分配。
     *
     * @param replicaIndexList 副本索引列表，表示哪些位置有副本分配。
     * @return LinkedList<Integer> 返回一个链表，表示分片分配情况。
     */
    private List<Integer> buildShardAllocationList(List<Integer> replicaIndexList) {
        List<Integer> shardAllocationList = new LinkedList<>();
        int startReplicaIndex = 1;
        // 遍历副本索引列表，生成分片分配列表
        for (Integer replicaIndex : replicaIndexList) {
            // 如果当前副本索引大于起始索引，则在两者之间填充0
            while (startReplicaIndex < replicaIndex) {
                shardAllocationList.add(0);
                startReplicaIndex++;
            }
            // 在当前位置添加1，表示有副本分配
            shardAllocationList.add(1);
            startReplicaIndex++;
        }
        return shardAllocationList;
    }


    /**
     * 构建副本索引列表，过滤掉需要移除的Pod索引，并返回一个去重且排序后的副本索引列表。
     * [1,3,5]代表最后需要pod1,pod3和pod5
     *
     * @param needRemovePodIndexSet 需要移除的Pod索引集合，包含所有需要排除的Pod索引。
     * @param adminShardList        管理员分片列表，包含所有分片及其副本信息。
     * @return 返回一个去重且按升序排序的副本索引列表。
     */
    private List<Integer> buildReplicaIndexList(Set<Integer> needRemovePodIndexSet, List<Shards> adminShardList) {
        List<Integer> replicaIndexList = new ArrayList<>();

        // 遍历所有分片及其副本，筛选出符合条件的副本索引
        for (Shards shard : adminShardList) {
            for (Replica replica : shard.getReplicas()) {
                Integer replicaIndex = Integer.valueOf(replica.getName());
                // 仅添加大于0且不在需要移除的Pod索引集合中的副本索引
                if (replicaIndex > 0 && !needRemovePodIndexSet.contains(replicaIndex)) {
                    replicaIndexList.add(replicaIndex);
                }
            }
        }
        replicaIndexList = replicaIndexList.stream().distinct().collect(Collectors.toList());
        replicaIndexList.sort((o1, o2) -> Integer.compare(o1, o2));
        return replicaIndexList;
    }
}
