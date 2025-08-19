package com.bilibili.cluster.scheduler.api.event.clickhouse;

import cn.hutool.json.JSONUtil;
import com.bilibili.cluster.scheduler.api.event.BatchedTaskEventHandler;
import com.bilibili.cluster.scheduler.api.service.bmr.metadata.BmrMetadataService;
import com.bilibili.cluster.scheduler.api.service.caster.ComCasterService;
import com.bilibili.cluster.scheduler.api.service.clickhouse.clickhouse.ClickhouseService;
import com.bilibili.cluster.scheduler.common.Constants;
import com.bilibili.cluster.scheduler.common.dto.bmr.metadata.MetadataClusterData;
import com.bilibili.cluster.scheduler.common.dto.bmr.metadata.MetadataPackageData;
import com.bilibili.cluster.scheduler.common.dto.caster.PodInfo;
import com.bilibili.cluster.scheduler.common.dto.clickhouse.clickhouse.params.CkContainerIterationFlowExtParams;
import com.bilibili.cluster.scheduler.common.dto.flow.ExecutionFlowProps;
import com.bilibili.cluster.scheduler.common.dto.flow.prop.BaseFlowExtPropDTO;
import com.bilibili.cluster.scheduler.common.entity.ExecutionFlowEntity;
import com.bilibili.cluster.scheduler.common.entity.ExecutionNodeEntity;
import com.bilibili.cluster.scheduler.common.enums.event.EventTypeEnum;
import com.bilibili.cluster.scheduler.common.enums.flow.FlowDeployType;
import com.bilibili.cluster.scheduler.common.enums.flow.FlowStatusEnum;
import com.bilibili.cluster.scheduler.common.event.TaskEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @description:
 * @Date: 2025/2/12 11:15
 * @Author: nizhiqiang
 */

@Slf4j
@Component
public class CKCheckContainerEventHandler extends BatchedTaskEventHandler {

    @Resource
    BmrMetadataService bmrMetadataService;

    @Resource
    ComCasterService comCasterService;

    @Resource
    ClickhouseService clickhouseService;

    private static Integer CHECK_MINUTES = 30;


    @Override
    public boolean batchExecEvent(TaskEvent taskEvent, List<ExecutionNodeEntity> nodeEntityList) throws Exception {

        logPersist(taskEvent, "等待1分钟");
        Thread.sleep(Constants.ONE_MINUTES);
        logPersist(taskEvent, "开始检查ck容器");
        ExecutionFlowProps executionFlowProps = taskEvent.getExecutionFlowInstanceDTO().getExecutionFlowProps();
        Long flowId = taskEvent.getFlowId();
        Long packageId = executionFlowProps.getPackageId();
        Long configId = executionFlowProps.getConfigId();
        Long clusterId = executionFlowProps.getClusterId();

        FlowDeployType deployType = executionFlowProps.getDeployType();
        BaseFlowExtPropDTO flowExtPropDTO = executionFlowPropsService.getFlowPropByFlowId(flowId, BaseFlowExtPropDTO.class);

        MetadataClusterData clusterData = bmrMetadataService.queryClusterDetail(clusterId);
        String appId = clusterData.getAppId();
        logPersist(taskEvent, "appId is : " + appId);
        LocalDateTime start = LocalDateTime.now();

        MetadataPackageData packageData = bmrMetadataService.queryPackageDetailById(packageId);
        String imagePath = packageData.getImagePath();

        while (Duration.between(start, LocalDateTime.now()).toMinutes() < CHECK_MINUTES) {
            ExecutionFlowEntity executionFlowEntity = executionFlowService.getById(taskEvent.getFlowId());
            FlowStatusEnum flowStatus = executionFlowEntity.getFlowStatus();
            if (FlowStatusEnum.isFinish(flowStatus)) {
                logPersist(taskEvent, "flow is terminate");
                return false;
            }

            String[] appIdSplit = appId.split("\\.");
            String clusterName = appIdSplit[appIdSplit.length - 1];

            String label = Constants.CK_ALTINTY_COM + "=" + clusterName;
            List<PodInfo> podInfoList = comCasterService.queryPodList(Constants.CK_K8S_CLUSTER_ID, label, null, null);

            if (!CollectionUtils.isEmpty(podInfoList)) {
                for (PodInfo podInfo : podInfoList) {
                    String image = podInfo.getImage();
                    String podName = podInfo.getName();
                    switch (deployType) {
//                        扩容需要所有节点的镜像都为发布的镜像
                        case K8S_CAPACITY_EXPANSION:
                            if (!image.equals(imagePath)) {
                                logPersist(taskEvent, "podName: " + podName + " image: " + image + " is not equal to imagePath: " + imagePath);
                                continue;
                            }
                            break;
//                        迭代需要迭代的节点镜像为发布的镜像
                        case K8S_ITERATION_RELEASE:
                            String flowExtParams = flowExtPropDTO.getFlowExtParams();
                            CkContainerIterationFlowExtParams ckContainerIterationFlowExtParams = JSONUtil.toBean(flowExtParams, CkContainerIterationFlowExtParams.class);
                            if (ckContainerIterationFlowExtParams.getIterationPodList().contains(podName) && !image.equals(imagePath)) {
                                logPersist(taskEvent, "podName: " + podName + " image: " + image + " is not equal to imagePath: " + imagePath);
                                continue;
                            }
                            break;
                    }
                    String podStatus = podInfo.getPodStatus();
                    ExecutionNodeEntity currentExecutionNode = getAndBindExecutionNodeByPodName(flowId, podName);
                    if (currentExecutionNode != null) {
                        executionNodeService.updatePodStatus(currentExecutionNode.getId(), podStatus);
                    }
                }
            }

            List<ExecutionNodeEntity> nodeList = executionNodeService.queryExecutionNodeByFlowId(flowId);
            boolean success = true;
            List<String> notFinishNodeList = new LinkedList<>();

            for (ExecutionNodeEntity executionNodeEntity : nodeList) {
                String podStatus = executionNodeEntity.getPodStatus();
                if (StringUtils.isEmpty(podStatus) || !Constants.POD_STATUS_SUCCESS.equals(podStatus)) {
                    notFinishNodeList.add(executionNodeEntity.getNodeName());
                    success = false;
                }
            }

            if (success) {
                logPersist(taskEvent, "ck容器检查完成");
                return true;
            }

            logPersist(taskEvent, "ck容器检查中，未完成节点列表为 " + notFinishNodeList);
            try {
                Thread.sleep(Constants.ONE_MINUTES);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        logPersist(taskEvent, "ck容器检查超时");
        return false;
    }

    private ExecutionNodeEntity getAndBindExecutionNodeByPodName(Long flowId, String podName) {
        clearDuplicatePodName(flowId);
        List<ExecutionNodeEntity> executionNodeList = executionNodeService.queryAllExecutionNodeByFlowId(flowId);
        Map<String, ExecutionNodeEntity> podNameToExecutionNodeMap = executionNodeList.stream().filter(executionNode -> !StringUtils.isEmpty(executionNode.getPodName()))
                .collect(Collectors.toMap(ExecutionNodeEntity::getPodName, Function.identity(), (o1, o2) -> o2));

//        已经绑定的中查询
        ExecutionNodeEntity currentExecutionNode = podNameToExecutionNodeMap.get(podName);
//        未绑定到节点则进行绑定，同名绑定
        if (currentExecutionNode == null) {
//            从未进行绑定的节点中查询type一致的pod进行绑定
            currentExecutionNode = executionNodeList.stream().filter(executionNode -> StringUtils.isEmpty(executionNode.getPodName()))
                    .filter(executionNode -> executionNode.getNodeName().equals(podName))
                    .findFirst()
                    .orElse(null);

//                    如果已经没有可以获取的则跳过该pod
            if (currentExecutionNode == null) {
                return null;
            }
            executionNodeService.updatePodName(currentExecutionNode.getId(), podName);
            podNameToExecutionNodeMap.put(podName, currentExecutionNode);
        }
        return currentExecutionNode;
    }


    /**
     * 清除重复的pod名
     *
     * @param flowId
     */
    private void clearDuplicatePodName(Long flowId) {
        List<ExecutionNodeEntity> executionNodeList = executionNodeService.queryExecutionNodeByFlowId(flowId);
        List<String> duplicatePodList = executionNodeList.stream()
                .collect(Collectors.groupingBy(ExecutionNodeEntity::getPodName, Collectors.counting()))
                .entrySet().stream()
                .filter(e -> e.getValue() > 1)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
        executionNodeService.clearPodByPodName(flowId, duplicatePodList);
    }

    @Override
    public int getMinLoopWait() {
        return 3_000;
    }

    @Override
    public int getMaxLoopStep() {
        return 100_000;
    }

    @Override
    public void printLog(TaskEvent taskEvent, String logContent) {

    }

    @Override
    public int logMod() {
        return 10;
    }

    @Override
    public EventTypeEnum getHandleEventType() {
        return EventTypeEnum.CK_CHECK_CONTAINER;
    }
}
