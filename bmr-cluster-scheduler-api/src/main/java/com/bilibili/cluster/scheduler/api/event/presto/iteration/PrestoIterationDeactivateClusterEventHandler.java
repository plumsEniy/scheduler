package com.bilibili.cluster.scheduler.api.event.presto.iteration;

import com.bilibili.cluster.scheduler.api.event.AbstractBranchedTaskEventHandler;
import com.bilibili.cluster.scheduler.api.service.bmr.metadata.BmrMetadataService;
import com.bilibili.cluster.scheduler.api.service.presto.PrestoService;
import com.bilibili.cluster.scheduler.common.Constants;
import com.bilibili.cluster.scheduler.common.dto.bmr.metadata.MetadataClusterData;
import com.bilibili.cluster.scheduler.common.dto.presto.iteration.PrestoIterationExtNodeParams;
import com.bilibili.cluster.scheduler.common.entity.ExecutionNodeEntity;
import com.bilibili.cluster.scheduler.common.entity.ExecutionNodeEventEntity;
import com.bilibili.cluster.scheduler.common.enums.event.EventStatusEnum;
import com.bilibili.cluster.scheduler.common.enums.event.EventTypeEnum;
import com.bilibili.cluster.scheduler.common.event.TaskEvent;
import com.google.common.base.Preconditions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.Duration;
import java.time.LocalDateTime;

@Slf4j
@Component
public class PrestoIterationDeactivateClusterEventHandler extends AbstractBranchedTaskEventHandler {

    @Resource
    PrestoService prestoService;

    @Resource
    BmrMetadataService metadataService;


    private static Integer CHECK_MINUTE = 5;

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

        Long clusterId = nodeParams.getClusterId();
        MetadataClusterData clusterData = metadataService.queryClusterDetail(clusterId);
        String clusterName = clusterData.getClusterName();
        String networkEnvironment = clusterData.getNetworkEnvironment().getEnv();

        prestoService.deactivateCluster(clusterName, networkEnvironment);
        logPersist(taskEvent, "deactivate cluster success");
        logPersist(taskEvent, "sleep 5 minutes");

        LocalDateTime startTime = LocalDateTime.now();

        while (Duration.between(startTime, LocalDateTime.now()).toMinutes() < CHECK_MINUTE) {
            try {
                ExecutionNodeEventEntity executionNodeEventEntity = executionNodeEventService.getById(taskEvent.getEventId());
                EventStatusEnum eventStatus = executionNodeEventEntity.getEventStatus();
                if (eventStatus.equals(EventStatusEnum.SKIPPED)) {
                    logPersist(taskEvent, "skip wait delete");
                    break;
                }
                Thread.sleep(Constants.ONE_SECOND * 10);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        logPersist(taskEvent, "continue execute");
        return true;
    }

    @Override
    public EventTypeEnum getHandleEventType() {
        return EventTypeEnum.PRESTO_ITERATION_DEACTIVATE_CLUSTER;
    }
}

