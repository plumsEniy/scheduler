package com.bilibili.cluster.scheduler.api.event.hdfs.nnproxy;

import com.bilibili.cluster.scheduler.api.event.AbstractTaskEventHandler;
import com.bilibili.cluster.scheduler.api.service.bmr.metadata.BmrMetadataService;
import com.bilibili.cluster.scheduler.api.service.bmr.resource.BmrResourceService;
import com.bilibili.cluster.scheduler.api.service.flow.ExecutionFlowPropsService;
import com.bilibili.cluster.scheduler.api.service.flow.ExecutionFlowService;
import com.bilibili.cluster.scheduler.api.service.flow.ExecutionNodePropsService;
import com.bilibili.cluster.scheduler.api.service.flow.ExecutionNodeService;
import com.bilibili.cluster.scheduler.api.service.flow.status.ExecuteFlowStatusProcess;
import com.bilibili.cluster.scheduler.api.service.wx.WxPublisherService;
import com.bilibili.cluster.scheduler.common.Constants;
import com.bilibili.cluster.scheduler.common.dto.bmr.metadata.MetadataComponentData;
import com.bilibili.cluster.scheduler.common.dto.bmr.resource.req.RefreshComponentReq;
import com.bilibili.cluster.scheduler.common.dto.bmr.resource.req.RefreshNodeListReq;
import com.bilibili.cluster.scheduler.common.dto.button.StageStateEnum;
import com.bilibili.cluster.scheduler.common.dto.flow.UpdateExecutionFlowDTO;
import com.bilibili.cluster.scheduler.common.dto.hdfs.nnproxy.parms.NNProxyDeployFlowExtParams;
import com.bilibili.cluster.scheduler.common.dto.hdfs.nnproxy.parms.NNProxyDeployNodeExtParams;
import com.bilibili.cluster.scheduler.common.entity.ExecutionFlowEntity;
import com.bilibili.cluster.scheduler.common.entity.ExecutionNodeEntity;
import com.bilibili.cluster.scheduler.common.enums.flow.FlowDeployType;
import com.bilibili.cluster.scheduler.common.enums.flow.FlowOperateButtonEnum;
import com.bilibili.cluster.scheduler.common.enums.flow.FlowRollbackType;
import com.bilibili.cluster.scheduler.common.enums.flow.FlowStatusEnum;
import com.bilibili.cluster.scheduler.common.enums.node.NodeType;
import com.bilibili.cluster.scheduler.common.event.TaskEvent;
import com.bilibili.cluster.scheduler.common.utils.ThreadUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;

import javax.annotation.Resource;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @description:
 * @Date: 2025/4/29 15:07
 * @Author: nizhiqiang
 */

@Slf4j
public abstract class AbstractNNproxyUpdateStateEventHandler extends AbstractTaskEventHandler {

    @Resource
    private BmrResourceService bmrResourceService;

    @Resource
    private ExecutionFlowService executionFlowService;

    @Resource
    private WxPublisherService wxPublisherService;

    @Resource
    ExecuteFlowStatusProcess executeFlowStatusProcess;

    @Resource
    ExecutionNodePropsService executionNodePropsService;

    @Resource
    ExecutionFlowPropsService executionFlowPropsService;

    @Resource
    BmrMetadataService bmrMetadataService;


    @Resource
    ExecutionNodeService executionNodeService;

    @Value("${nnproxy.deploy.stage.waitTs}")
    long stageWaitTs;

    /**
     * 如果为true则会在逻辑节点后暂停
     *
     * @return
     */
    protected boolean logicNodeStop() {
        return true;
    }

    @Override
    public boolean executeTaskEvent(TaskEvent taskEvent) throws Exception {
        ExecutionNodeEntity executionNode = taskEvent.getExecutionNode();
        Long flowId = taskEvent.getFlowId();
        ExecutionFlowEntity executionFlow = executionFlowService.getById(flowId);
        NodeType nodeType = executionNode.getNodeType();
        final Long nodeId = executionNode.getId();
        switch (nodeType) {
            case STAGE_END_NODE:
                // 更新stage信息
                LocalDateTime now = LocalDateTime.now();
                LocalDateTime allowedNextStageStartTime = now.plus(stageWaitTs, ChronoUnit.MILLIS);
                executionFlowPropsService.updateStageInfo(flowId, executionNode.getExecStage(),
                        StageStateEnum.SUCCESS, null, now, allowedNextStageStartTime);
                logPersist(taskEvent, "更新发布阶段信息完成");

                // 阶段结束的逻辑节点暂停工作流
                if (logicNodeStop()) {
                    final String maxStage = executionNodeService.queryMaxStageByFlowId(flowId);
                    String execStage = executionNode.getExecStage();
                    if (maxStage.equals(execStage)) {
                        String message = String.format("该逻辑节点是最终阶段%s最后节点，发布任务完成。", executionNode.getExecStage());
                        logPersist(taskEvent, message);
                        return true;
                    }

//                    NNProxyDeployNodeExtParams nodeExtParams = executionNodePropsService.queryNodePropsByNodeId(nodeId, NNProxyDeployNodeExtParams.class);
//                    if (Objects.isNull(nodeExtParams)) {
//                        nodeExtParams = new NNProxyDeployNodeExtParams();
//                        nodeExtParams.setNodeId(nodeId);
//                        nodeExtParams.setStartTime(LocalDateTime.now());
//                        nodeExtParams.setFilled(Boolean.TRUE);
//                        executionNodePropsService.saveNodeProp(nodeId, nodeExtParams);
//                    }
//                    LocalDateTime startTime = nodeExtParams.getStartTime();
//                    // 当前需要等待12小时之后才能再开启下个发布阶段
//                    waitWithStageDeploy(taskEvent, startTime, nodeId);

                    String message = String.format("该逻辑节点是当前阶段%s最后节点，将暂停发布任务。", executionNode.getExecStage());
                    logPersist(taskEvent, message);

                    final UpdateExecutionFlowDTO updateExecutionFlowDTO = new UpdateExecutionFlowDTO();
                    updateExecutionFlowDTO.setFlowId(flowId);
                    updateExecutionFlowDTO.setFlowStatus(FlowStatusEnum.PAUSED);
                    updateExecutionFlowDTO.setRollbackType(FlowRollbackType.NONE);
                    executionFlowService.updateFlow(updateExecutionFlowDTO);
                    bmrFlowService.alterFlowStatus(flowId, FlowOperateButtonEnum.PAUSE);
                    String flowDetailUrl = executionFlowService.generateFlowUrl(executionFlow);
                    String clusterName = executionFlow.getClusterName();
                    message = String.format("集群%s的nnproxy全量迭代第%s阶段完成，工作流暂停。工作流链接%s", clusterName, executionNode.getExecStage(), flowDetailUrl);
                    wxPublisherService.wxPushMsg(Arrays.asList(executionFlow.getOperator()), Constants.MSG_TYPE_TEXT, message);
                }
                break;
            case STAGE_START_NODE:
                logPersist(taskEvent, "阶段开始阶段无任何操作");
                break;
            case NORMAL:
                //        正常节点更新节点发布状态（判断是否回滚）
                NNProxyDeployNodeExtParams nnProxyDeployNodeExtParams = executionNodePropsService.queryNodePropsByNodeId(executionNode.getId(), NNProxyDeployNodeExtParams.class);

                String configVersion = getConfigVersion(taskEvent, nnProxyDeployNodeExtParams);
                String packageVersion = getPackageVersion(taskEvent, nnProxyDeployNodeExtParams);

                NNProxyDeployFlowExtParams flowProps = executionFlowPropsService.getFlowExtParamsByCache(flowId, NNProxyDeployFlowExtParams.class);
                FlowDeployType deployType = flowProps.getSubDeployType().getFlowDeployType();
                logPersist(taskEvent, "更新节点发布状态,发布类型为" + deployType);

                Long componentId = getComponentId(taskEvent, nnProxyDeployNodeExtParams);
                MetadataComponentData metadataComponentData = bmrMetadataService.queryComponentByComponentId(componentId);

                Long clusterId = executionFlow.getClusterId();

                bmrResourceService.updateNodeListState(clusterId, componentId,
                        Arrays.asList(executionNode.getNodeName()), deployType, true, packageVersion, configVersion);


                // update resource-v2 node status
                RefreshNodeListReq refreshNodeManagerNodeListReq = new RefreshNodeListReq();
                refreshNodeManagerNodeListReq.setHostList(Arrays.asList(executionNode.getNodeName()));
                refreshNodeManagerNodeListReq.setClusterId(clusterId);
                refreshNodeManagerNodeListReq.setDeployTypeEnum(deployType.name());

                List<RefreshComponentReq> refreshComponentReqList = new ArrayList<>();
                RefreshComponentReq refreshResourceV2Req = new RefreshComponentReq();
                refreshResourceV2Req.setComponentId(componentId);
                refreshResourceV2Req.setComponentName(metadataComponentData.getComponentName());
                refreshResourceV2Req.setPackageDiskVersion(packageVersion);
                refreshResourceV2Req.setConfigDiskVersion(configVersion);
                refreshComponentReqList.add(refreshResourceV2Req);
                refreshNodeManagerNodeListReq.setRefreshComponentReqList(refreshComponentReqList);

                globalService.getBmrResourceV2Service().refreshDeployNodeInfo(refreshNodeManagerNodeListReq);
                break;
        }

        return true;
    }

    private void waitWithStageDeploy(TaskEvent taskEvent, LocalDateTime startTime, Long nodeId) {
        int index = 0;
        ExecutionNodeEntity nodeEntity = executionNodeService.getById(nodeId);
        String msg;
        while (nodeEntity.getNodeStatus().isInExecute()) {
            final LocalDateTime now = LocalDateTime.now();
            final long waitIntervalTs = Duration.between(startTime, now).abs().toMillis();
            if (waitIntervalTs >= stageWaitTs) {
                break;
            }
            // 等待10s
            ThreadUtils.sleep(Constants.ONE_SECOND * 10);
            if (index % 100 == 0) {
                msg = "Currently in the release phase waiting state....(当前处于阶段完成发布等待中)";
                msg += String.format("总等待时间: %s分钟, 还需等待时间: %s分钟.", stageWaitTs / Constants.ONE_MINUTES, waitIntervalTs / Constants.ONE_MINUTES);
                logPersist(taskEvent, msg);
            }
            index++;
            nodeEntity = executionNodeService.getById(nodeId);
        }
    }

    protected abstract String getConfigVersion(TaskEvent taskEvent, NNProxyDeployNodeExtParams nnProxyDeployNodeExtParams);

    protected abstract String getPackageVersion(TaskEvent taskEvent, NNProxyDeployNodeExtParams nnProxyDeployNodeExtParams);

    protected abstract Long getComponentId(TaskEvent taskEvent, NNProxyDeployNodeExtParams nnProxyDeployNodeExtParams);
}
