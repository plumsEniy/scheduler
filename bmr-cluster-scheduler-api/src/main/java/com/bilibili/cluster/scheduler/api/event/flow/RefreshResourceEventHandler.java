package com.bilibili.cluster.scheduler.api.event.flow;

import com.bilibili.cluster.scheduler.api.service.bmr.config.BmrConfigService;
import com.bilibili.cluster.scheduler.api.service.bmr.metadata.BmrMetadataService;
import com.bilibili.cluster.scheduler.api.service.bmr.resource.BmrResourceService;
import com.bilibili.cluster.scheduler.api.service.bmr.resourceV2.BmrResourceV2Service;
import com.bilibili.cluster.scheduler.common.Constants;
import com.bilibili.cluster.scheduler.common.dto.bmr.config.ConfigDetailData;
import com.bilibili.cluster.scheduler.common.dto.bmr.metadata.MetadataPackageData;
import com.bilibili.cluster.scheduler.common.dto.bmr.resource.req.RefreshComponentReq;
import com.bilibili.cluster.scheduler.common.dto.bmr.resource.req.RefreshNodeListReq;
import com.bilibili.cluster.scheduler.common.entity.ExecutionFlowEntity;
import com.bilibili.cluster.scheduler.common.entity.ExecutionNodeEntity;
import com.bilibili.cluster.scheduler.common.enums.NodeExecuteStatusEnum;
import com.bilibili.cluster.scheduler.common.enums.flow.FlowAopEventType;
import com.bilibili.cluster.scheduler.common.enums.flow.FlowDeployType;
import com.bilibili.cluster.scheduler.common.enums.node.NodeType;
import com.bilibili.cluster.scheduler.common.utils.NumberUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * @description: 刷新资源管理系统
 * @Date: 2025/5/12 17:11
 * @Author: nizhiqiang
 */

@Component
public class RefreshResourceEventHandler implements AbstractFlowAopEventHandler {

    @Resource
    BmrResourceService bmrResourceService;

    @Resource
    BmrResourceV2Service bmrResourceV2Service;

    @Resource
    BmrMetadataService bmrMetadataService;

    @Resource
    BmrConfigService bmrConfigService;

    @Override
    public boolean jobFinish(ExecutionFlowEntity executionFlow, ExecutionNodeEntity executionNode) {
        NodeType nodeType = executionNode.getNodeType();
//        只有普通节点才会触发
        if (!NodeType.NORMAL.equals(nodeType)){
            return true;
        }

        FlowDeployType deployType = executionFlow.getDeployType();

        FlowDeployType proxyDeployType = getResourceProxyDeployType(deployType);

        NodeExecuteStatusEnum nodeNodeExecuteStatus = executionNode.getNodeStatus();
        boolean isSuccess = nodeNodeExecuteStatus.isSuccessExecute();
        Long clusterId = executionFlow.getClusterId();
        Long componentId = executionFlow.getComponentId();

        Long packageId = executionFlow.getPackageId();
        Long configId = executionFlow.getConfigId();

        String packageDiskVersion = getPackageDiskVersion(packageId);
        String configDiskVersion = getConfigDiskVersion(configId);

        List<String> nodeList = new ArrayList<>();
        nodeList.add(executionNode.getNodeName());
        bmrResourceService.updateNodeListState(clusterId
                , componentId, nodeList, proxyDeployType, isSuccess,
                packageDiskVersion, configDiskVersion);

        RefreshNodeListReq refreshNodeListReq = new RefreshNodeListReq();
        refreshNodeListReq.setHostList(Arrays.asList(executionNode.getNodeName()));
        refreshNodeListReq.setClusterId(clusterId);
        refreshNodeListReq.setDeployTypeEnum(proxyDeployType.name());

        List<RefreshComponentReq> refreshComponentReqList = new ArrayList<>();
        RefreshComponentReq refreshComponentReq = new RefreshComponentReq();
        refreshComponentReq.setComponentId(componentId);
        refreshComponentReq.setComponentName(executionFlow.getComponentName());
        refreshComponentReq.setPackageDiskVersion(packageDiskVersion);
        refreshComponentReq.setConfigDiskVersion(configDiskVersion);
        refreshComponentReqList.add(refreshComponentReq);
        refreshNodeListReq.setRefreshComponentReqList(refreshComponentReqList);

        bmrResourceV2Service.refreshDeployNodeInfo(refreshNodeListReq);

        return true;
    }

    private FlowDeployType getResourceProxyDeployType(FlowDeployType deployType) {
        switch (deployType) {
            case NNPROXY_RESTART:
                return FlowDeployType.RESTART_SERVICE;
            default:
                return deployType;
        }
    }

    private String getPackageDiskVersion(long packageId) {
        if (!NumberUtils.isPositiveLong(packageId)) {
            return Constants.EMPTY_STRING;
        }

        MetadataPackageData metadataPackage = bmrMetadataService.queryPackageDetailById(packageId);
        if (Objects.isNull(metadataPackage)) {
            return Constants.EMPTY_STRING;
        }

        return metadataPackage.getTagName();
    }

    private String getConfigDiskVersion(long configId) {
        if (!NumberUtils.isPositiveLong(configId)) {
            return Constants.EMPTY_STRING;
        }

        ConfigDetailData configData = bmrConfigService.queryConfigDetailById(configId);
        if (Objects.isNull(configData)) {
            return Constants.EMPTY_STRING;
        }

        return configData.getConfigVersionNumber();
    }

    @Override
    public FlowAopEventType getEventType() {
        return FlowAopEventType.REFRESH_RESOURCE;
    }
}
