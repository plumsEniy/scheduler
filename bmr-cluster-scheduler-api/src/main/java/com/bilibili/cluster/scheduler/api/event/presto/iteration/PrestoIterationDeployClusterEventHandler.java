package com.bilibili.cluster.scheduler.api.event.presto.iteration;


import com.bilibili.cluster.scheduler.api.event.AbstractBranchedTaskEventHandler;
import com.bilibili.cluster.scheduler.api.service.bmr.metadata.BmrMetadataService;
import com.bilibili.cluster.scheduler.api.service.presto.PrestoService;
import com.bilibili.cluster.scheduler.common.dto.bmr.metadata.MetadataPackageData;
import com.bilibili.cluster.scheduler.common.dto.presto.iteration.PrestoIterationExtNodeParams;
import com.bilibili.cluster.scheduler.common.entity.ExecutionNodeEntity;
import com.bilibili.cluster.scheduler.common.enums.event.EventTypeEnum;
import com.bilibili.cluster.scheduler.common.event.TaskEvent;
import com.google.common.base.Preconditions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Objects;

@Slf4j
@Component
public class PrestoIterationDeployClusterEventHandler extends AbstractBranchedTaskEventHandler {

    @Resource
    PrestoService prestoService;

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
        Long configId = nodeParams.getConfigId();
        Long packageId = nodeParams.getPackId();
        MetadataPackageData packageData = metadataService.queryPackageDetailById(packageId);
        if (Objects.isNull(packageData)) {
            throw new RuntimeException("can not find metadata package, package id is " + packageId);
        }
        String imagePath = packageData.getImagePath();

        String casterTemplate = prestoService.getDeployPrestoTemplate(clusterId, configId, imagePath);
        logPersist(taskEvent, " yaml is: " + prestoService.queryPrestoTemplate(clusterId, configId, imagePath));
        logPersist(taskEvent, " caster template is: " + casterTemplate);

        prestoService.deployPresto(clusterId, configId, imagePath);
        logPersist(taskEvent, "deploy presto success");
        return true;
    }

    @Override
    public EventTypeEnum getHandleEventType() {
        return EventTypeEnum.PRESTO_ITERATION_DEPLOY_CLUSTER;
    }

}
