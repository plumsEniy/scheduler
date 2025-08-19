package com.bilibili.cluster.scheduler.api.event.resource;

import cn.hutool.json.JSONUtil;
import com.bilibili.cluster.scheduler.api.event.BatchedTaskEventHandler;
import com.bilibili.cluster.scheduler.common.dto.bmr.config.ConfigDetailData;
import com.bilibili.cluster.scheduler.common.dto.bmr.metadata.MetadataPackageData;
import com.bilibili.cluster.scheduler.common.dto.bmr.resource.req.RefreshComponentReq;
import com.bilibili.cluster.scheduler.common.dto.bmr.resource.req.RefreshNodeListReq;
import com.bilibili.cluster.scheduler.common.entity.ExecutionFlowEntity;
import com.bilibili.cluster.scheduler.common.entity.ExecutionNodeEntity;
import com.bilibili.cluster.scheduler.common.enums.event.EventTypeEnum;
import com.bilibili.cluster.scheduler.common.event.TaskEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Component
public class RefreshResourceInfoHandler extends BatchedTaskEventHandler {

    @Override
    public EventTypeEnum getHandleEventType() {
        return EventTypeEnum.REFRESH_RESOURCE_MANAGER_INFO_EVENT;
    }

    @Override
    public boolean batchExecEvent(TaskEvent taskEvent, List<ExecutionNodeEntity> nodeEntityList) throws Exception {
        Long flowId = taskEvent.getFlowId();
        ExecutionFlowEntity flowEntity = executionFlowService.getById(flowId);

        RefreshNodeListReq refreshNodeListReq = new RefreshNodeListReq();
        refreshNodeListReq.setClusterId(flowEntity.getClusterId());
        refreshNodeListReq.setDeployTypeEnum(flowEntity.getDeployType().name());
        refreshNodeListReq.setHostList(nodeEntityList.stream()
                .map(ExecutionNodeEntity::getNodeName).collect(Collectors.toList()));

        RefreshComponentReq refreshComponentReq = new RefreshComponentReq();
        refreshComponentReq.setComponentId(flowEntity.getComponentId());
        refreshComponentReq.setComponentName(flowEntity.getComponentName());
        Long packageId = flowEntity.getPackageId();
        if (!Objects.isNull(packageId) && packageId > 0) {
            MetadataPackageData metadataPackageData = globalService.getBmrMetadataService().queryPackageDetailById(flowEntity.getPackageId());
            refreshComponentReq.setPackageDiskVersion(metadataPackageData.getTagName());
        }
        Long configId = flowEntity.getConfigId();
        if (!Objects.isNull(configId) && configId > 0) {
            ConfigDetailData configDetailData = globalService.getBmrConfigService().queryConfigDetailById(configId);
            refreshComponentReq.setConfigDiskVersion(configDetailData.getConfigVersionNumber());
        }

        List<RefreshComponentReq> refreshComponentReqList = Arrays.asList(refreshComponentReq);
        refreshNodeListReq.setRefreshComponentReqList(refreshComponentReqList);

        log(taskEvent,
                "request to bmr resource manager data is: " + JSONUtil.toJsonStr(refreshNodeListReq));
        return funcRetry(3, globalService.getBmrResourceV2Service()::refreshDeployNodeInfo, refreshNodeListReq);
    }

    @Override
    public int getMinLoopWait() {
        return 3_000;
    }

    @Override
    public int getMaxLoopStep() {
        return 10_000;
    }

    @Override
    public void printLog(TaskEvent taskEvent, String logContent) {
        log.info(logContent);
    }

    @Override
    public int logMod() {
        return 10;
    }
}
