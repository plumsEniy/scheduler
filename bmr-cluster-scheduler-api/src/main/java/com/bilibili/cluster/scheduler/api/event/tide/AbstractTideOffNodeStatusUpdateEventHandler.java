package com.bilibili.cluster.scheduler.api.event.tide;

import cn.hutool.core.thread.ThreadUtil;
import com.bilibili.cluster.scheduler.api.event.AbstractTaskEventHandler;
import com.bilibili.cluster.scheduler.api.service.GlobalService;
import com.bilibili.cluster.scheduler.api.service.flow.ExecutionFlowPropsService;
import com.bilibili.cluster.scheduler.common.Constants;
import com.bilibili.cluster.scheduler.common.dto.bmr.config.ConfigDetailData;
import com.bilibili.cluster.scheduler.common.dto.bmr.metadata.MetadataComponentData;
import com.bilibili.cluster.scheduler.common.dto.bmr.metadata.MetadataPackageData;
import com.bilibili.cluster.scheduler.common.dto.bmr.resource.req.RefreshComponentReq;
import com.bilibili.cluster.scheduler.common.dto.bmr.resource.req.RefreshNodeListReq;
import com.bilibili.cluster.scheduler.common.dto.bmr.resourceV2.TideNodeDetail;
import com.bilibili.cluster.scheduler.common.dto.tide.flow.TideExtFlowParams;
import com.bilibili.cluster.scheduler.common.entity.ExecutionNodeEntity;
import com.bilibili.cluster.scheduler.common.enums.flow.FlowDeployType;
import com.bilibili.cluster.scheduler.common.enums.resourceV2.TideClusterType;
import com.bilibili.cluster.scheduler.common.enums.resourceV2.TideNodeStatus;
import com.bilibili.cluster.scheduler.common.event.TaskEvent;
import com.bilibili.cluster.scheduler.common.utils.NumberUtils;
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
public abstract class AbstractTideOffNodeStatusUpdateEventHandler extends AbstractTaskEventHandler {

    @Resource
    GlobalService globalService;

    @Resource
    ExecutionFlowPropsService flowPropsService;

    /**
     * 是否跳过taskevent的检查
     *
     * @return
     */
    protected abstract boolean skipCheckEventIsRequired();

    protected TideExtFlowParams getTideFlowExtParams(long flowId) {
        return flowPropsService.getFlowExtParamsByCache(flowId, TideExtFlowParams.class);
    }

    protected abstract TideClusterType getTideClusterType();

    @Override
    public boolean executeTaskEvent(TaskEvent taskEvent) throws Exception {
        TideExtFlowParams tideFlowExtParams = getTideFlowExtParams(taskEvent.getFlowId());

        String hostname = taskEvent.getExecutionNode().getNodeName();
        String appId = getAppId(taskEvent, tideFlowExtParams);
        final TideNodeDetail nodeDetail = globalService.getBmrResourceV2Service().queryTideNodeDetail(hostname);
        if (Objects.isNull(nodeDetail)) {
            logPersist(taskEvent, "hostname is not found in bmr resource: " + hostname);
            return false;
        }

        final long yarnClusterId = tideFlowExtParams.getYarnClusterId();
        globalService.getBmrResourceV2Service().updateTideNodeServiceAndStatus(
                hostname, TideNodeStatus.STAIN, appId, "NodeManager,SparkEssWorker,amiya", getTideClusterType());

        // 切换nodeLabel
        String nodeLabel = Constants.TIDE_ON_YARN_NODE_LABEL;
        String msg;
//         RetryUtils.retryWith(3, 3, () ->
//                 globalService.getBmrResourceService().switchYarnNodeLabel(yarnClusterId, hostname, nodeLabel));
        int curRetry = 0;
        int maxRetry = 3;
        boolean isSuccess = false;
        while (curRetry++ < maxRetry) {
            try {
                globalService.getBmrResourceService().switchYarnNodeLabel(yarnClusterId, hostname, nodeLabel);
                msg = String.format("潮汐节点[%s]切换yarn集群nodeLabel至[%s]成功", hostname, nodeLabel);
                logPersist(taskEvent, msg);
                isSuccess = true;
                break;
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                logPersist(taskEvent, "NM切换node-label失败，原因：" + e.getMessage());
                msg = String.format("潮汐节点[%s]切换yarn集群nodeLabel至[%s]失败，请检查", hostname, nodeLabel);
                logPersist(taskEvent, msg);
            }
            ThreadUtil.safeSleep(curRetry * 3000);
        }
        if (!isSuccess) {
            return false;
        }

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

        // update nm node status and pack、conf version
        final long nodeManagerComponentId = nodeManagerComponentData.getId();
        long nodeManagerDefaultPackId = globalService.getBmrMetadataService().queryDefaultPackageIdByComponentId(nodeManagerComponentId);
        final MetadataPackageData nodeManagerDefaultPackageData = globalService.getBmrMetadataService().queryPackageDetailById(nodeManagerDefaultPackId);
        final long nodeManagerDefaultConfId = globalService.getBmrConfigService().queryDefaultConfigVersionIdByComponentId(nodeManagerComponentId);
        final ConfigDetailData nodeManagerDefaultConfData = globalService.getBmrConfigService().queryConfigDetailById(nodeManagerDefaultConfId);

        final RefreshComponentReq nodeManagerRefreshComponentReq = new RefreshComponentReq();
        nodeManagerRefreshComponentReq.setComponentId(nodeManagerComponentId);
        nodeManagerRefreshComponentReq.setComponentName(nodeManagerComponentData.getComponentName());
        nodeManagerRefreshComponentReq.setPackageDiskVersion(nodeManagerDefaultPackageData.getTagName());
        nodeManagerRefreshComponentReq.setConfigDiskVersion(nodeManagerDefaultConfData.getConfigVersionNumber());
        refreshComponentReqList.add(nodeManagerRefreshComponentReq);

        globalService.getBmrResourceService().updateNodeListState(yarnClusterId, nodeManagerComponentId,
                hostnameList, FlowDeployType.CAPACITY_EXPANSION, true, nodeManagerDefaultPackageData.getTagName(), nodeManagerDefaultConfData.getConfigVersionNumber());

        // update spark ess worker node status and pack、conf version
        final long sparkEssWorkerComponentId = sparkEssWorkerComponentData.getId();
        long sparkEssWorkerDefaultPackageId = globalService.getBmrMetadataService().queryDefaultPackageIdByComponentId(sparkEssWorkerComponentId);
        final MetadataPackageData sparkEssWorkerDefaultPackInfo = globalService.getBmrMetadataService().queryPackageDetailById(sparkEssWorkerDefaultPackageId);
        long sparkEssWorkerDefaultConfId = globalService.getBmrConfigService().queryDefaultConfigVersionIdByComponentId(sparkEssWorkerComponentId);

        final RefreshComponentReq sparkEssWorkerRefreshComponentReq = new RefreshComponentReq();
        sparkEssWorkerRefreshComponentReq.setComponentId(sparkEssWorkerComponentId);
        sparkEssWorkerRefreshComponentReq.setComponentName(sparkEssWorkerComponentData.getComponentName());
        sparkEssWorkerRefreshComponentReq.setPackageDiskVersion(sparkEssWorkerDefaultPackInfo.getTagName());
        refreshComponentReqList.add(sparkEssWorkerRefreshComponentReq);
        String sparkEssWorkerDefaultConfVersion = "";
        if (NumberUtils.isPositiveLong(sparkEssWorkerDefaultConfId)) {
            final ConfigDetailData sparkEssWorkerDefaultConfData = globalService.getBmrConfigService().queryConfigDetailById(sparkEssWorkerDefaultConfId);
            sparkEssWorkerDefaultConfVersion = sparkEssWorkerDefaultConfData.getConfigVersionNumber();
            sparkEssWorkerRefreshComponentReq.setConfigDiskVersion(sparkEssWorkerDefaultConfVersion);
        }
        globalService.getBmrResourceService().updateNodeListState(yarnClusterId, sparkEssWorkerComponentId, hostnameList,
                FlowDeployType.CAPACITY_EXPANSION, true, sparkEssWorkerDefaultPackInfo.getTagName(), sparkEssWorkerDefaultConfVersion);

        // update amiya node status and pack、conf version
        long amiyaComponentId = amiyaComponentData.getId();
        long amiyaDefaultPackageId = globalService.getBmrMetadataService().queryDefaultPackageIdByComponentId(amiyaComponentId);

        final RefreshComponentReq amiyaRefreshComponentReq = new RefreshComponentReq();
        amiyaRefreshComponentReq.setComponentId(amiyaComponentId);
        amiyaRefreshComponentReq.setComponentName(amiyaComponentData.getComponentName());
        String amiyaDefaultPackVersion = "";
        if (NumberUtils.isPositiveLong(amiyaDefaultPackageId)) {
            final MetadataPackageData amiyaDefaultPackInfo = globalService.getBmrMetadataService().queryPackageDetailById(amiyaDefaultPackageId);
            amiyaDefaultPackVersion = amiyaDefaultPackInfo.getTagName();
        }
        amiyaRefreshComponentReq.setPackageDiskVersion(amiyaDefaultPackVersion);

        String amiyaDefaultConfigVersion = "";
        long amiyaDefaultConfigId = globalService.getBmrConfigService().queryDefaultConfigVersionIdByComponentId(amiyaComponentId);
        if (NumberUtils.isPositiveLong(amiyaDefaultConfigId)) {
            final ConfigDetailData amiyaDefaultConfigInfo = globalService.getBmrConfigService().queryConfigDetailById(amiyaDefaultConfigId);
            amiyaDefaultConfigVersion = amiyaDefaultConfigInfo.getConfigVersionNumber();
        }
        amiyaRefreshComponentReq.setConfigDiskVersion(amiyaDefaultConfigVersion);
        globalService.getBmrResourceService().updateNodeListState(yarnClusterId, amiyaComponentId, hostnameList,
                FlowDeployType.CAPACITY_EXPANSION, true, amiyaDefaultPackVersion, amiyaDefaultConfigVersion);

        // update resource-v2 node status and pack version
        final RefreshNodeListReq refreshNodeManagerNodeListReq = new RefreshNodeListReq();
        refreshNodeManagerNodeListReq.setHostList(hostnameList);
        refreshNodeManagerNodeListReq.setClusterId(yarnClusterId);
        refreshNodeManagerNodeListReq.setDeployTypeEnum(FlowDeployType.CAPACITY_EXPANSION.name());
        refreshNodeManagerNodeListReq.setRefreshComponentReqList(refreshComponentReqList);

        globalService.getBmrResourceV2Service().refreshDeployNodeInfo(refreshNodeManagerNodeListReq);
        return true;
    }

    protected String getAppId(TaskEvent taskEvent, TideExtFlowParams tideFlowExtParams) {
        String appId = tideFlowExtParams.getAppId();
        return appId;
    }

    /**
     * 仅在阶段2执行
     *
     * @param taskEvent
     * @return
     */
    @Override
    protected boolean checkEventIsRequired(TaskEvent taskEvent) {

        //        是否跳过检查
        if (skipCheckEventIsRequired()) {
            return true;
        }

        final ExecutionNodeEntity executionNode = taskEvent.getExecutionNode();
        final String execStage = executionNode.getExecStage();
        if (execStage.equalsIgnoreCase("1")) {
            return false;
        } else {
            return true;
        }
    }
}
