package com.bilibili.cluster.scheduler.api.event.presto.iteration;

import com.bilibili.cluster.scheduler.api.event.AbstractBranchedTaskEventHandler;
import com.bilibili.cluster.scheduler.api.service.bmr.metadata.BmrMetadataService;
import com.bilibili.cluster.scheduler.api.service.caster.ComCasterService;
import com.bilibili.cluster.scheduler.api.service.presto.PrestoService;
import com.bilibili.cluster.scheduler.common.Constants;
import com.bilibili.cluster.scheduler.common.dto.bmr.metadata.MetadataClusterData;
import com.bilibili.cluster.scheduler.common.dto.bmr.metadata.MetadataPackageData;
import com.bilibili.cluster.scheduler.common.dto.caster.PodInfo;
import com.bilibili.cluster.scheduler.common.dto.presto.iteration.PrestoIterationExtNodeParams;
import com.bilibili.cluster.scheduler.common.dto.presto.template.PrestoCasterConfig;
import com.bilibili.cluster.scheduler.common.dto.presto.template.PrestoCasterTemplate;
import com.bilibili.cluster.scheduler.common.dto.presto.template.PrestoDeployDTO;
import com.bilibili.cluster.scheduler.common.entity.ExecutionFlowEntity;
import com.bilibili.cluster.scheduler.common.entity.ExecutionNodeEntity;
import com.bilibili.cluster.scheduler.common.enums.event.EventTypeEnum;
import com.bilibili.cluster.scheduler.common.enums.flow.FlowStatusEnum;
import com.bilibili.cluster.scheduler.common.event.TaskEvent;
import com.google.common.base.Preconditions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Component
public class PrestoGlobalCheckClusterEventHandler extends AbstractBranchedTaskEventHandler {

    @Resource
    ComCasterService comCasterService;

    @Resource
    PrestoService prestoService;

    @Resource
    BmrMetadataService metadataService;

    private static Integer CHECK_MINUTES = 30;

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

    /**
     * 处理普通节点，不存在回滚分支的场景
     * @param taskEvent
     * @return
     */
    @Override
    protected boolean executeNormalNodeTaskEvent(TaskEvent taskEvent) throws Exception {
        final Long executionNodeId = taskEvent.getExecutionNodeId();
        PrestoIterationExtNodeParams nodeParams = executionNodePropsService.queryNodePropsByNodeId(executionNodeId, PrestoIterationExtNodeParams.class);
        Preconditions.checkNotNull(nodeParams, "nodeParams is null");

        long clusterId = nodeParams.getClusterId();
        MetadataClusterData clusterData = metadataService.queryClusterDetail(clusterId);
        String appId = clusterData.getAppId();
        Long configId = nodeParams.getConfigId();
        Long packageId = nodeParams.getPackId();
        MetadataPackageData packageData = metadataService.queryPackageDetailById(packageId);
        if (Objects.isNull(packageData)) {
            throw new RuntimeException("can not find metadata package, package id is " + packageId);
        }

        Long flowId = taskEvent.getFlowId();
        String imagePath = packageData.getImagePath();
        String networkEnvironment = clusterData.getNetworkEnvironment().getEnv();
        LocalDateTime start = LocalDateTime.now();
        PrestoDeployDTO prestoDeployDTO = prestoService.generateDeployPrestoReq(clusterId, configId, imagePath);

        Map<String, Integer> typeToNeedCountMap = new HashMap<>();
        PrestoCasterTemplate template = prestoDeployDTO.getTemplate();
        PrestoCasterConfig worker = template.getWorker();
        PrestoCasterConfig coordinator = template.getCoordinator();
        PrestoCasterConfig resource = template.getResource();
        Integer workerCount = worker.getCount();
        typeToNeedCountMap.put(Constants.WORKER, workerCount);

        if (coordinator != null) {
            typeToNeedCountMap.put(Constants.COORDINATOR, coordinator.getCount());
        }
        if (resource != null) {
            typeToNeedCountMap.put(Constants.RESOURCE, resource.getCount());
        }

        logPersist(taskEvent, String.format("need pod count map is %s", typeToNeedCountMap));

        ExecutionFlowEntity executionFlow = executionFlowService.getById(flowId);
        LocalDateTime startTime = executionFlow.getStartTime();

        while (Duration.between(start, LocalDateTime.now()).toMinutes() < CHECK_MINUTES) {

            ExecutionFlowEntity executionFlowEntity = executionFlowService.getById(taskEvent.getFlowId());
            FlowStatusEnum flowStatus = executionFlowEntity.getFlowStatus();
            if (FlowStatusEnum.isFinish(flowStatus)) {
                logPersist(taskEvent, "flow is terminate");
                return false;
            }

            String podSelector = String.format("app_id=%s,env=%s", appId, networkEnvironment);
            List<PodInfo> podInfoList = comCasterService.queryPodList(prestoService.getPrestoCasterClusterId(), podSelector, null, null);
            podInfoList = podInfoList.stream().filter(podInfo -> podInfo.getTime().isAfter(startTime)).collect(Collectors.toList());

            Map<String, Integer> typeToSuccessMap = new HashMap<>();
            if (!CollectionUtils.isEmpty(podInfoList)) {
                for (PodInfo podInfo : podInfoList) {
                    String podStatus = podInfo.getPodStatus();
                    if (!Constants.POD_STATUS_SUCCESS.equals(podStatus)) {
                        continue;
                    }
                    String podType = podInfo.getPodType();
                    typeToSuccessMap.put(podType, typeToSuccessMap.getOrDefault(podType, 0) + 1);
                }
            }

            logPersist(taskEvent, String.format("current pod count map is %s", typeToSuccessMap));
            boolean success = true;
            if (typeToSuccessMap.isEmpty()) {
                success = false;
            } else {
                for (String type : typeToNeedCountMap.keySet()) {
                    Integer successCount = Optional.ofNullable(typeToSuccessMap.get(type)).orElse(0);
                    Integer needCount = Optional.ofNullable(typeToNeedCountMap.get(type)).orElse(0);
                    boolean checkResult = checkPodCount(taskEvent, type, successCount, needCount);
                    if (!checkResult) {
                        success = false;
                        break;
                    }
                }
            }

            if (success) {
                logPersist(taskEvent, "pod capacity success");
                return true;
            }

            try {
                Thread.sleep(Constants.ONE_MINUTES);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        logPersist(taskEvent, "execute over time");
        return false;
    }

    private boolean checkPodCount(TaskEvent taskEvent, String type, Integer successCount, Integer needCount) {
        logPersist(taskEvent, String.format("type %s, need count is %s, current count is %s", type, needCount, successCount));
        if (!needCount.equals(successCount)) {
            logPersist(taskEvent, String.format("type %s count not equals", type));
            return false;
        }
        return true;
    }

    @Override
    public EventTypeEnum getHandleEventType() {
        return EventTypeEnum.PRESTO_ITERATION_GLOBAL_CHECK_CLUSTER;
    }

}
