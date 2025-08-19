package com.bilibili.cluster.scheduler.api.event.presto.iteration;

import com.bilibili.cluster.scheduler.api.event.AbstractBranchedTaskEventHandler;
import com.bilibili.cluster.scheduler.api.service.bmr.metadata.BmrMetadataService;
import com.bilibili.cluster.scheduler.api.service.caster.CasterService;
import com.bilibili.cluster.scheduler.common.Constants;
import com.bilibili.cluster.scheduler.common.dto.bmr.metadata.MetadataClusterData;
import com.bilibili.cluster.scheduler.common.dto.caster.resp.BaseComResp;
import com.bilibili.cluster.scheduler.common.dto.presto.iteration.PrestoIterationExtNodeParams;
import com.bilibili.cluster.scheduler.common.entity.ExecutionNodeEntity;
import com.bilibili.cluster.scheduler.common.enums.event.EventTypeEnum;
import com.bilibili.cluster.scheduler.common.event.TaskEvent;
import com.bilibili.cluster.scheduler.common.utils.BaseRespUtil;
import com.google.common.base.Preconditions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;


@Slf4j
@Component
public class PrestoIterationDeletedClusterEventHandler extends AbstractBranchedTaskEventHandler {

    @Resource
    CasterService casterService;

    @Resource
    BmrMetadataService metadataService;

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
        String networkEnvironment = clusterData.getNetworkEnvironment().getEnv();
        BaseComResp resp = casterService.deletePresto(clusterData.getAppId(), networkEnvironment, Constants.PRESTO_CASTER_CLUSTER_NAME);
        String message = resp.getMessage();
        if (message.contains("cannot be found") || message.contains("not found") || message.contains("can't find application")) {
            logPersist(taskEvent, "cluster cannot be found, will skip");
            return true;
        }
        BaseRespUtil.checkComResp(resp);
        logPersist(taskEvent, "delete caster cluster success");
        return true;
    }

    @Override
    public EventTypeEnum getHandleEventType() {
        return EventTypeEnum.PRESTO_ITERATION_DELETED_CLUSTER;
    }
}

