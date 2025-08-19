package com.bilibili.cluster.scheduler.api.event.spark.periphery;

import cn.hutool.json.JSONUtil;
import com.bilibili.cluster.scheduler.api.event.AbstractBranchedTaskEventHandler;
import com.bilibili.cluster.scheduler.api.service.bmr.spark.SparkManagerService;
import com.bilibili.cluster.scheduler.common.dto.spark.periphery.SparkPeripheryComponent;
import com.bilibili.cluster.scheduler.common.dto.spark.periphery.SparkPeripheryComponentDeployFlowExtParams;
import com.bilibili.cluster.scheduler.common.dto.spark.params.SparkPeripheryComponentDeployJobExtParams;
import com.bilibili.cluster.scheduler.common.dto.spark.periphery.req.SparkPeripheryComponentVersionInfoReq;
import com.bilibili.cluster.scheduler.common.dto.spark.periphery.resp.SparkPeripheryComponentVersionInfoDTO;
import com.bilibili.cluster.scheduler.common.entity.ExecutionNodeEntity;
import com.bilibili.cluster.scheduler.common.enums.event.EventStatusEnum;
import com.bilibili.cluster.scheduler.common.enums.event.EventTypeEnum;
import com.bilibili.cluster.scheduler.common.enums.node.NodeOperationResult;
import com.bilibili.cluster.scheduler.common.event.TaskEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Objects;

@Slf4j
@Component
public class SparkPeripheryComponentDeployPreCheckEventHandler extends AbstractBranchedTaskEventHandler {

    @Resource
    SparkManagerService sparkManagerService;

    @Override
    public boolean hasRollbackBranch() {
        return true;
    }

    /**
     * 处理普通节点，并处于正向处理的逻辑
     * @param taskEvent
     * @return
     */
    public boolean executeNormalNodeOnForwardStateTaskEvent(TaskEvent taskEvent) throws Exception {
        final ExecutionNodeEntity executionNode = taskEvent.getExecutionNode();
        final String jobId = executionNode.getNodeName();
        final Long flowId = taskEvent.getFlowId();
        final SparkPeripheryComponentDeployFlowExtParams flowExtParams = executionFlowPropsService.getFlowExtParamsByCache(flowId, SparkPeripheryComponentDeployFlowExtParams.class);
        final SparkPeripheryComponent peripheryComponent = flowExtParams.getPeripheryComponent();
        final SparkPeripheryComponentDeployJobExtParams jobExtParams = new SparkPeripheryComponentDeployJobExtParams();
        jobExtParams.setJobId(jobId);
        jobExtParams.setPeripheryComponent(peripheryComponent);
        final SparkPeripheryComponentVersionInfoReq req = new SparkPeripheryComponentVersionInfoReq(peripheryComponent, jobId);
        final SparkPeripheryComponentVersionInfoDTO componentVersionInfoDTO = sparkManagerService.querySparkPeripheryComponentVersionInfo(req);
        jobExtParams.setNodeId(executionNode.getId());
        jobExtParams.setJobName(componentVersionInfoDTO.getJobName());
        jobExtParams.setOriginalVersion(componentVersionInfoDTO.getTargetVersion());
        jobExtParams.setTargetVersion(flowExtParams.getTargetVersion());
        jobExtParams.setLocked(componentVersionInfoDTO.isLocked());
        logPersist(taskEvent, "jobExtParams is: " + JSONUtil.toJsonStr(jobExtParams));
        // save job ext props
        executionNodePropsService.saveNodeProp(executionNode.getId(), jobExtParams);

        if (componentVersionInfoDTO.isLocked()) {
            logPersist(taskEvent, "periphery component version is locked, skip...");
            taskEvent.setEventStatus(EventStatusEnum.SKIPPED);
            taskEvent.setOperationResult(NodeOperationResult.SPARK_PERIPHERY_COMPONENT_VERSION_LOCKED);
            return true;
        }
        String message = String.format("will set spark periphery component [%s] version from [%s] to [%s]",
                peripheryComponent,
                jobExtParams.getOriginalVersion(), jobExtParams.getTargetVersion());
        logPersist(taskEvent, message);
        return true;
    }

    /**
     * 处理普通节点，并处于回滚状态的逻辑
     * @param taskEvent
     * @return
     */
    public boolean executeNormalNodeOnRollbackStateTaskEvent(TaskEvent taskEvent) throws Exception {
        final ExecutionNodeEntity executionNode = taskEvent.getExecutionNode();
        final Long nodeId = executionNode.getId();
        final SparkPeripheryComponentDeployJobExtParams deployJobExtParams = executionNodePropsService.queryNodePropsByNodeId(nodeId, SparkPeripheryComponentDeployJobExtParams.class);
        if (Objects.isNull(deployJobExtParams)) {
            logPersist(taskEvent, "deployJobExtParams is null, nodeId is: " + nodeId);
            return false;
        }

        if (deployJobExtParams.isLocked()) {
            logPersist(taskEvent, "periphery component version is locked, skip...");
            taskEvent.setEventStatus(EventStatusEnum.SKIPPED);
            taskEvent.setOperationResult(NodeOperationResult.SPARK_PERIPHERY_COMPONENT_VERSION_LOCKED);
            return true;
        }

        String message = String.format("periphery component of [%s] will rollback version from [%s] to [%s]",
                deployJobExtParams.getPeripheryComponent(),
                deployJobExtParams.getTargetVersion(), deployJobExtParams.getOriginalVersion());
        logPersist(taskEvent, message);
        return true;
    }


    @Override
    public EventTypeEnum getHandleEventType() {
        return EventTypeEnum.SPARK_PERIPHERY_COMPONENT_DEPLOY_PRE_CHECK;
    }

}
