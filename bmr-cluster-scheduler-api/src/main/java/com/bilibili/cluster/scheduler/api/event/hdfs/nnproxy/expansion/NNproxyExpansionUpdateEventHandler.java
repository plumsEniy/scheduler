package com.bilibili.cluster.scheduler.api.event.hdfs.nnproxy.expansion;

import com.bilibili.cluster.scheduler.api.event.hdfs.nnproxy.AbstractNNproxyUpdateStateEventHandler;
import com.bilibili.cluster.scheduler.api.service.bmr.config.BmrConfigService;
import com.bilibili.cluster.scheduler.api.service.bmr.metadata.BmrMetadataService;
import com.bilibili.cluster.scheduler.common.dto.bmr.config.ConfigDetailData;
import com.bilibili.cluster.scheduler.common.dto.bmr.metadata.MetadataPackageData;
import com.bilibili.cluster.scheduler.common.dto.hdfs.nnproxy.parms.NNProxyDeployNodeExtParams;
import com.bilibili.cluster.scheduler.common.enums.event.EventTypeEnum;
import com.bilibili.cluster.scheduler.common.event.TaskEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import javax.annotation.Resource;

/**
 * @description:
 * @Date: 2025/4/29 15:42
 * @Author: nizhiqiang
 */

@Slf4j
@Component
public class NNproxyExpansionUpdateEventHandler extends AbstractNNproxyUpdateStateEventHandler {

    @Resource
    BmrConfigService bmrConfigService;

    @Resource
    BmrMetadataService bmrMetadataService;

    @Override
    protected boolean logicNodeStop() {
        return false;
    }

    @Override
    protected String getConfigVersion(TaskEvent taskEvent, NNProxyDeployNodeExtParams nnProxyDeployNodeExtParams) {
        Long configId = taskEvent.getExecutionFlowInstanceDTO().getExecutionFlowProps().getConfigId();
        ConfigDetailData configDetailData = bmrConfigService.queryConfigDetailById(configId);
        Assert.notNull(configDetailData, String.format("无法查询到该节点的组件配置信息,配置id为%s", configId));
        return configDetailData.getConfigVersionNumber();
    }

    @Override
    protected String getPackageVersion(TaskEvent taskEvent, NNProxyDeployNodeExtParams nnProxyDeployNodeExtParams) {
        Long packageId = taskEvent.getExecutionFlowInstanceDTO().getExecutionFlowProps().getPackageId();
        MetadataPackageData metadataPackageData = bmrMetadataService.queryPackageDetailById(packageId);
        Assert.notNull(metadataPackageData, String.format("无法查询到该节点的组件安装包信息,安装包id为%s", packageId));
        return metadataPackageData.getTagName();
    }

    @Override
    protected Long getComponentId(TaskEvent taskEvent, NNProxyDeployNodeExtParams nnProxyDeployNodeExtParams) {
        return taskEvent.getExecutionFlowInstanceDTO().getExecutionFlowProps().getComponentId();
    }

    @Override
    public EventTypeEnum getHandleEventType() {
        return EventTypeEnum.NN_PROXY_EXPANSION_UPDATE_STATE;
    }
}
