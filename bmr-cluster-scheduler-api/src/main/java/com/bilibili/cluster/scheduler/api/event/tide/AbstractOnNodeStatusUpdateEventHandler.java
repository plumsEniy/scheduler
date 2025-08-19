package com.bilibili.cluster.scheduler.api.event.tide;

import com.bilibili.cluster.scheduler.api.event.AbstractTaskEventHandler;
import com.bilibili.cluster.scheduler.api.service.GlobalService;
import com.bilibili.cluster.scheduler.api.service.caster.ComCasterService;
import com.bilibili.cluster.scheduler.api.service.flow.ExecutionFlowPropsService;
import com.bilibili.cluster.scheduler.common.dto.bmr.metadata.MetadataComponentData;
import com.bilibili.cluster.scheduler.common.dto.bmr.resource.req.RefreshComponentReq;
import com.bilibili.cluster.scheduler.common.dto.bmr.resource.req.RefreshNodeListReq;
import com.bilibili.cluster.scheduler.common.dto.bmr.resourceV2.TideNodeDetail;
import com.bilibili.cluster.scheduler.common.dto.tide.flow.TideExtFlowParams;
import com.bilibili.cluster.scheduler.common.entity.ExecutionNodeEntity;
import com.bilibili.cluster.scheduler.common.enums.flow.FlowDeployType;
import com.bilibili.cluster.scheduler.common.enums.resourceV2.TideClusterType;
import com.bilibili.cluster.scheduler.common.enums.resourceV2.TideNodeStatus;
import com.bilibili.cluster.scheduler.common.event.TaskEvent;
import com.google.common.base.Preconditions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@Slf4j
@Component
public abstract class AbstractOnNodeStatusUpdateEventHandler extends AbstractTaskEventHandler {

    @Resource
    GlobalService globalService;

    @Resource
    ExecutionFlowPropsService flowPropsService;

    @Resource
    ComCasterService comCasterService;

    protected TideExtFlowParams getTideFlowExtParams(long flowId) {
        return flowPropsService.getFlowExtParamsByCache(flowId, TideExtFlowParams.class);
    }

    protected abstract long getTideOffCasterClusterId();

    protected abstract TideClusterType getTideClusterType();

    protected abstract String getDeployService();

    @Override
    public boolean executeTaskEvent(TaskEvent taskEvent) throws Exception {
        try {
            TideExtFlowParams tideFlowExtParams = getTideFlowExtParams(taskEvent.getFlowId());

            String hostname = taskEvent.getExecutionNode().getNodeName();
            final long yarnClusterId = tideFlowExtParams.getYarnClusterId();
            final TideNodeDetail nodeDetail = globalService.getBmrResourceV2Service().queryTideNodeDetail(hostname);
            if (Objects.isNull(nodeDetail)) {
                logPersist(taskEvent, "hostname is not found in bmr resource: " + hostname);
                return false;
            }
            // 更新下线成功节点状态至可调度状态（关闭污点调度）
            boolean isSuc = comCasterService.updateNodeToTaintOff(getTideOffCasterClusterId(), nodeDetail.getIp(), getTideClusterType());
            Preconditions.checkState(isSuc, "com caster updateNodeToTaintOff failed");
            logPersist(taskEvent, "更新com平台下线成功节点状态至可调度状态（关闭污点调度）完成,节点名称: " + hostname);
            // 更新资源池节点状态至可用状态
            globalService.getBmrResourceV2Service().updateTideNodeServiceAndStatus(
                    hostname, TideNodeStatus.AVAILABLE, "", getDeployService(), getTideClusterType());
            logPersist(taskEvent, "bmr更新潮汐节点状态至可调度状态,节点名称: " + hostname);

            final List<MetadataComponentData> componentDataList = globalService.getBmrMetadataService()
                    .queryComponentListByClusterId(yarnClusterId);

            MetadataComponentData nodeManagerComponentData = null;
            MetadataComponentData sparkEssWorkerComponentData = null;
            MetadataComponentData amiyaComponentData = null;

            for (MetadataComponentData metadataComponentData : componentDataList) {
                final String componentName = metadataComponentData.getComponentName();
                switch (componentName) {
                    case "NodeManager":
                        nodeManagerComponentData = metadataComponentData;
                        break;
                    case "Amiya":
                        amiyaComponentData = metadataComponentData;
                        break;
                    case "SparkEssWorker":
                        sparkEssWorkerComponentData = metadataComponentData;
                        break;
                    default:
                        continue;
                }
            }

            Preconditions.checkNotNull(nodeManagerComponentData, "NodeManager metadata not exist");
            Preconditions.checkNotNull(sparkEssWorkerComponentData, "SparkEssWorker metadata not exist");
            Preconditions.checkNotNull(amiyaComponentData, "Amiya metadata not exist");

            List<RefreshComponentReq> refreshComponentReqList = new ArrayList<>();
            List<String> hostnameList = Arrays.asList(hostname);

            // update nm node status to offline
            final long nodeManagerComponentId = nodeManagerComponentData.getId();
            globalService.getBmrResourceService().updateNodeListState(yarnClusterId, nodeManagerComponentId,
                    hostnameList, FlowDeployType.OFF_LINE_EVICTION, true, "", "");
            final RefreshComponentReq nodeManagerRefreshReq = new RefreshComponentReq();
            nodeManagerRefreshReq.setComponentId(nodeManagerComponentId);
            nodeManagerRefreshReq.setComponentName(nodeManagerComponentData.getComponentName());
            nodeManagerRefreshReq.setPackageDiskVersion("");
            nodeManagerRefreshReq.setConfigDiskVersion("");
            refreshComponentReqList.add(nodeManagerRefreshReq);

            //  update spark ess worker node status to offline
            final long sparkEssWorkerComponentId = sparkEssWorkerComponentData.getId();
            globalService.getBmrResourceService().updateNodeListState(yarnClusterId, sparkEssWorkerComponentId,
                    hostnameList, FlowDeployType.OFF_LINE_EVICTION, true, "", "");
            final RefreshComponentReq sparkEssWorkerRefreshReq = new RefreshComponentReq();
            sparkEssWorkerRefreshReq.setComponentId(sparkEssWorkerComponentId);
            sparkEssWorkerRefreshReq.setComponentName(sparkEssWorkerComponentData.getComponentName());
            sparkEssWorkerRefreshReq.setPackageDiskVersion("");
            sparkEssWorkerRefreshReq.setConfigDiskVersion("");
            refreshComponentReqList.add(sparkEssWorkerRefreshReq);

            //  update amiya node status to offline
            final long amiyaComponentId = amiyaComponentData.getId();
            globalService.getBmrResourceService().updateNodeListState(yarnClusterId, amiyaComponentId,
                    hostnameList, FlowDeployType.OFF_LINE_EVICTION, true, "", "");
            final RefreshComponentReq amiyaRefreshReq = new RefreshComponentReq();
            amiyaRefreshReq.setComponentId(amiyaComponentId);
            amiyaRefreshReq.setComponentName(amiyaComponentData.getComponentName());
            amiyaRefreshReq.setPackageDiskVersion("");
            amiyaRefreshReq.setConfigDiskVersion("");
            refreshComponentReqList.add(amiyaRefreshReq);

            // update resource-v2 node status
            final RefreshNodeListReq refreshNodeManagerNodeListReq = new RefreshNodeListReq();
            refreshNodeManagerNodeListReq.setHostList(hostnameList);
            refreshNodeManagerNodeListReq.setClusterId(yarnClusterId);
            refreshNodeManagerNodeListReq.setDeployTypeEnum(FlowDeployType.OFF_LINE_EVICTION.name());
            refreshNodeManagerNodeListReq.setRefreshComponentReqList(refreshComponentReqList);

            globalService.getBmrResourceV2Service().refreshDeployNodeInfo(refreshNodeManagerNodeListReq);
            return true;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            String message = getHandleEventType().getDesc() + "执行失败，原因：" + e.getMessage();
            logPersist(taskEvent, message);
            throw e;
        }
    }

    /**
     * 潮汐上线节点服务状态更新，仅在stage1执行
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
}
