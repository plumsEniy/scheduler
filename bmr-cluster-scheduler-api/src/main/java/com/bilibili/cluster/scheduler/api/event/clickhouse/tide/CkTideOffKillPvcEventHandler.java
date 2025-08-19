package com.bilibili.cluster.scheduler.api.event.clickhouse.tide;

import com.bilibili.cluster.scheduler.api.event.AbstractTaskEventHandler;
import com.bilibili.cluster.scheduler.api.service.caster.ComCasterService;
import com.bilibili.cluster.scheduler.api.service.flow.ExecutionFlowPropsService;
import com.bilibili.cluster.scheduler.api.service.flow.ExecutionFlowService;
import com.bilibili.cluster.scheduler.api.service.flow.ExecutionNodeService;
import com.bilibili.cluster.scheduler.common.Constants;
import com.bilibili.cluster.scheduler.common.dto.caster.PodInfo;
import com.bilibili.cluster.scheduler.common.dto.caster.PvcInfo;
import com.bilibili.cluster.scheduler.common.dto.caster.PvcMetadata;
import com.bilibili.cluster.scheduler.common.dto.clickhouse.clickhouse.params.CkTideExtFlowParams;
import com.bilibili.cluster.scheduler.common.entity.ExecutionFlowEntity;
import com.bilibili.cluster.scheduler.common.entity.ExecutionNodeEntity;
import com.bilibili.cluster.scheduler.common.enums.event.EventTypeEnum;
import com.bilibili.cluster.scheduler.common.enums.flow.FlowStatusEnum;
import com.bilibili.cluster.scheduler.common.event.TaskEvent;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @description: ck潮汐删除pvc
 * @Date: 2025/4/10 10:54
 * @Author: nizhiqiang
 */

@Slf4j
@Component
public class CkTideOffKillPvcEventHandler extends AbstractTaskEventHandler {

    private static Integer CHECK_MINUTES = 30;


    @Resource
    ExecutionFlowService executionFlowService;

    @Resource
    ComCasterService comCasterService;

    @Resource
    ExecutionFlowPropsService flowPropsService;

    @Resource
    ExecutionNodeService nodeService;

    @Override
    public boolean executeTaskEvent(TaskEvent taskEvent) throws Exception {

        ExecutionNodeEntity executionNode = taskEvent.getExecutionNode();

        final String execStage = executionNode.getExecStage();
        if (execStage.equalsIgnoreCase("2")) {
            return normalNodeKillPvc(taskEvent);
        } else {
            return logicNodeKillPvc(taskEvent);
        }

    }

    /**
     * 删除潮汐过程中已经删除pod，但是主机未被选中潮汐，对这些主机上的潮汐pode下的pvc进行删除
     *
     * @param taskEvent
     * @return
     */
    private boolean logicNodeKillPvc(TaskEvent taskEvent) {
        Map<String, Set<String>> nameSpaceToPvcListMap = new HashMap<>();
        Long flowId = taskEvent.getFlowId();
        final CkTideExtFlowParams ckTideExtFlowParams = flowPropsService.getFlowExtParamsByCache(flowId, CkTideExtFlowParams.class);
        Map<String, Set<String>> hostToRemovePodMap = ckTideExtFlowParams.getHostIpToRemovePodMap();

        if (CollectionUtils.isEmpty(hostToRemovePodMap)) {
            logPersist(taskEvent, "hostToRemovePodMap is empty, skip kill pvc");
            return true;
        }

        List<ExecutionNodeEntity> executionNodeList = executionNodeService.queryExecutionNodeByFlowId(flowId);
        Set<String> normalNodeIpSet = executionNodeList.stream().filter(node -> node.getExecStage().equalsIgnoreCase("2")).map(ExecutionNodeEntity::getIp).collect(Collectors.toSet());

        for (Map.Entry<String, Set<String>> entry : hostToRemovePodMap.entrySet()) {
            String hostIp = entry.getKey();
            Set<String> podNameSet = entry.getValue();
            if (normalNodeIpSet.contains(hostIp)) {
                logPersist(taskEvent, String.format("%s主机上的pod列表为空，跳过该主机", hostIp));
                continue;
            }

//            pvc中会包含pod的完整名称，需要过滤掉podNameSet中不包含的pvc
            List<PvcInfo> pvcInfoList = comCasterService.queryPvcListByHost(Constants.CK_K8S_CLUSTER_ID, hostIp, null, null);
            if (CollectionUtils.isEmpty(pvcInfoList)) {
                logPersist(taskEvent, String.format("%s主机上的pvc列表为空，跳过该主机", hostIp));
                continue;
            }

            pvcInfoList = pvcInfoList.stream().filter(pvcInfo -> {
                String pvcName = pvcInfo.getMetadata().getName();
                for (String podName : podNameSet) {
                    if (pvcName.contains(podName)) {
                        return true;
                    }
                }
                return false;
            }).collect(Collectors.toList());

            List<String> pvcNameList = pvcInfoList.stream().map(PvcInfo::getMetadata).map(PvcMetadata::getName).collect(Collectors.toList());
            logPersist(taskEvent, String.format("host is %s, pod name set is %s, pvc list is %s", hostIp, podNameSet, pvcNameList));

            for (PvcInfo pvcInfo : pvcInfoList) {
                PvcMetadata metadata = pvcInfo.getMetadata();
                String pvcName = metadata.getName();
                String namespace = metadata.getNamespace();
                Set<String> pvcSet = nameSpaceToPvcListMap.getOrDefault(namespace, new HashSet<>());
                pvcSet.add(pvcName);
                nameSpaceToPvcListMap.put(namespace, pvcSet);
            }
        }


        logPersist(taskEvent, "--------------开始删除pvc--------------");

        for (Map.Entry<String, Set<String>> entry : nameSpaceToPvcListMap.entrySet()) {
            String namespace = entry.getKey();
            Set<String> pvcSet = entry.getValue();
            logPersist(taskEvent, String.format("删除的namespace为%s,pvc列表为%s", namespace, pvcSet));
            List<String> pvcList = new LinkedList<>();
            pvcList.addAll(pvcSet);
            comCasterService.deletePvc(Constants.CK_K8S_CLUSTER_ID, namespace, pvcList);
            logPersist(taskEvent, "删除成功");
        }

        return true;
    }

    private boolean normalNodeKillPvc(TaskEvent taskEvent) throws InterruptedException {
        Long flowId = taskEvent.getFlowId();
        ExecutionNodeEntity executionNode = taskEvent.getExecutionNode();

        //        发起删除pvc请求操作
        String nodeName = executionNode.getNodeName();
        List<PodInfo> ckPodList = queryCkPodListByExecutionNode(executionNode);
        if (!CollectionUtils.isEmpty(ckPodList)) {
            List<String> ckPodNameList = ckPodList.stream().map(PodInfo::getName).collect(Collectors.toList());
            logPersist(taskEvent, String.format("ck pod list is not empty,host is %s, pod name list is %s", nodeName, ckPodNameList));
            return false;
        }

        List<PvcInfo> pvcInfoList = comCasterService.queryPvcListByHost(Constants.CK_K8S_CLUSTER_ID, executionNode.getIp(), null, null);
        if (CollectionUtils.isEmpty(pvcInfoList)) {
            logPersist(taskEvent, String.format("pvc list is empty, skip kill"));
            return true;
        }
        Map<String, List<PvcMetadata>> nameSpaceToPvcMetadataMap = pvcInfoList.stream().map(PvcInfo::getMetadata).collect(Collectors.groupingBy(PvcMetadata::getNamespace));
        for (Map.Entry<String, List<PvcMetadata>> entry : nameSpaceToPvcMetadataMap.entrySet()) {
            String nameSpace = entry.getKey();
            List<String> pvcNameList = entry.getValue().stream().map(PvcMetadata::getName).collect(Collectors.toList());
            logPersist(taskEvent, String.format("host is %s, name space is %s, deleted pvc name list is %s", nodeName, nameSpace, pvcNameList));
            try {
                comCasterService.deletePvc(Constants.CK_K8S_CLUSTER_ID, nameSpace, pvcNameList);
            } catch (Exception e) {
                throw new RuntimeException(String.format("delete pvc error, host is %s, name space is %s, pvc name list is %s,error msg is %s", nodeName, nameSpace, pvcNameList, e.getMessage()), e);
            }
        }
        LocalDateTime startTime = LocalDateTime.now();
        logPersist(taskEvent, "---------start check pvc--------------");


        while (LocalDateTime.now().isBefore(startTime.plusMinutes(CHECK_MINUTES))) {
//            防止工作流结单还在删pvc
            ExecutionFlowEntity executionFlow = executionFlowService.getById(flowId);
            FlowStatusEnum flowStatus = executionFlow.getFlowStatus();
            if (flowStatus.isFinish()) {
                logPersist(taskEvent, "flow is terminate");
                return false;
            }

            pvcInfoList = comCasterService.queryPvcListByHost(Constants.CK_K8S_CLUSTER_ID, executionNode.getIp(), null, null);
            if (CollectionUtils.isEmpty(pvcInfoList)) {
                logPersist(taskEvent, String.format("host is %s, pvc list is empty", executionNode.getNodeName()));
                logPersist(taskEvent, "---------end check pvc--------------");
                return true;
            }
            List<String> pvcNameList = pvcInfoList.stream().map(PvcInfo::getMetadata).map(PvcMetadata::getName).collect(Collectors.toList());

            logPersist(taskEvent, String.format(" pvc list is %s, not empty, sleep 2 minutes", pvcNameList));
            Thread.sleep(Constants.ONE_MINUTES * 2);
        }

        logPersist(taskEvent, "清除pvc超时");
        return false;
    }

    /**
     * 是否包含ck或者keeper容器
     *
     * @param currentPodInfoList
     * @return
     */
    private List<PodInfo> queryCkPodListByExecutionNode(ExecutionNodeEntity executionNode) {

        List<PodInfo> podInfoList = comCasterService.queryPodListByNodeIp(Constants.CK_K8S_CLUSTER_ID, executionNode.getIp());

        if (CollectionUtils.isEmpty(podInfoList)) {
            return Collections.emptyList();
        }

        LinkedList<PodInfo> ckPodList = new LinkedList<>();

        for (PodInfo podInfo : podInfoList) {
            final String name = podInfo.getName();
            if (StringUtils.isBlank(name)) {
                continue;
            }
            if (name.contains("keeper")) {
                ckPodList.add(podInfo);
                continue;
            }
            final Map<String, String> labels = podInfo.getLabels();
            if (CollectionUtils.isEmpty(labels)) {
                continue;
            }
            final String scene = labels.getOrDefault("scene", "");
            if (scene.equalsIgnoreCase("clickhouse") || scene.equalsIgnoreCase("clickhouse-keeper")) {
                ckPodList.add(podInfo);
                continue;
            }
        }
        return ckPodList;
    }

    @Override
    public EventTypeEnum getHandleEventType() {
        return EventTypeEnum.CK_TIDE_OFF_KILL_PVC;
    }
}
