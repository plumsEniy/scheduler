package com.bilibili.cluster.scheduler.api.event.hdfs.nnproxy.iteration;

import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.bilibili.cluster.scheduler.api.event.AbstractTaskEventHandler;
import com.bilibili.cluster.scheduler.api.service.bmr.config.BmrConfigService;
import com.bilibili.cluster.scheduler.api.service.bmr.metadata.BmrMetadataService;
import com.bilibili.cluster.scheduler.api.service.bmr.resource.BmrResourceService;
import com.bilibili.cluster.scheduler.api.service.flow.ExecutionNodePropsService;
import com.bilibili.cluster.scheduler.api.service.flow.ExecutionNodeService;
import com.bilibili.cluster.scheduler.common.Constants;
import com.bilibili.cluster.scheduler.common.dto.bmr.config.ComponentConfigVersionEntity;
import com.bilibili.cluster.scheduler.common.dto.bmr.config.ConfigDetailData;
import com.bilibili.cluster.scheduler.common.dto.bmr.metadata.InstallationPackage;
import com.bilibili.cluster.scheduler.common.dto.bmr.metadata.MetadataComponentData;
import com.bilibili.cluster.scheduler.common.dto.bmr.metadata.MetadataPackageData;
import com.bilibili.cluster.scheduler.common.dto.bmr.metadata.req.QueryInstallationPackageListReq;
import com.bilibili.cluster.scheduler.common.dto.bmr.resource.ComponentNodeDetail;
import com.bilibili.cluster.scheduler.common.dto.bmr.resource.req.QueryComponentNodeListReq;
import com.bilibili.cluster.scheduler.common.dto.button.StageStateEnum;
import com.bilibili.cluster.scheduler.common.dto.hdfs.nnproxy.parms.NNProxyDeployNodeExtParams;
import com.bilibili.cluster.scheduler.common.entity.ExecutionNodeEntity;
import com.bilibili.cluster.scheduler.common.enums.event.EventStatusEnum;
import com.bilibili.cluster.scheduler.common.enums.event.EventTypeEnum;
import com.bilibili.cluster.scheduler.common.enums.node.NodeExecType;
import com.bilibili.cluster.scheduler.common.enums.node.NodeType;
import com.bilibili.cluster.scheduler.common.event.TaskEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * @description: nnproxy预检查
 * @Date: 2025/4/28 14:36
 * @Author: nizhiqiang
 */

@Slf4j
@Component
public class NNproxyIterationPreCheckEventHandler extends AbstractTaskEventHandler {

    @Resource
    private ExecutionNodePropsService executionNodePropsService;

    @Resource
    private BmrMetadataService bmrMetadataService;

    @Resource
    private BmrConfigService bmrConfigService;

    @Resource
    private BmrResourceService bmrResourceService;

    @Resource
    private ExecutionNodeService executionNodeService;

    @Override
    public boolean executeTaskEvent(TaskEvent taskEvent) throws Exception {
        final NodeType nodeType = taskEvent.getExecutionNode().getNodeType();
        if (!nodeType.isNormalExecNode()) {
            if (nodeType == NodeType.STAGE_START_NODE) {
                executionFlowPropsService.updateStageInfo(taskEvent.getFlowId(),
                        taskEvent.getExecutionNode().getExecStage(),
                        StageStateEnum.RUNNING,
                        LocalDateTime.now(),
                        null,
                        null);
            }
            logPersist(taskEvent, nodeType.getDesc() + ", 跳过执行");
            return true;
        }

        ExecutionNodeEntity executionNode = taskEvent.getExecutionNode();
        Long nodeId = executionNode.getId();
        NNProxyDeployNodeExtParams nnProxyDeployNodeExtParams = executionNodePropsService.queryNodePropsByNodeId(nodeId, NNProxyDeployNodeExtParams.class);
        boolean filled = nnProxyDeployNodeExtParams.isFilled();
        if (!filled) {
            logPersist(taskEvent, "未填充过属性，进行属性填充");
            fillProps(taskEvent, nnProxyDeployNodeExtParams);
        } else {
            logPersist(taskEvent, "已填充过属性跳过属性填充");
        }

        executionNode = executionNodeService.getById(nodeId);
        NodeExecType execType = executionNode.getExecType();
        if (NodeExecType.WAITING_ROLLBACK.equals(execType)) {
            logPersist(taskEvent, "该节点为回滚状态，跳过后续所有事件");
            taskEvent.setEventStatus(EventStatusEnum.SKIPPED);
        }

        return true;
    }

    /**
     * 填充属性
     *
     * @param taskEvent
     * @param nnProxyDeployNodeExtParams
     */
    private void fillProps(TaskEvent taskEvent, NNProxyDeployNodeExtParams nnProxyDeployNodeExtParams) {
        ExecutionNodeEntity executionNode = taskEvent.getExecutionNode();
        Long nodeId = executionNode.getId();
        Long componentId = nnProxyDeployNodeExtParams.getComponentId();
        Long clusterId = taskEvent.getExecutionFlowInstanceDTO().getExecutionFlowProps().getClusterId();

        ComponentNodeDetail componentNodeDetail = queryNodeComponentDetail(executionNode, componentId, clusterId);
        if (Objects.isNull(componentNodeDetail)) {
            String errorMsg = String.format("无法查询到该节点的组件下信息,集群id%s,组件id%s", clusterId, componentId);
            throw new RuntimeException(errorMsg);
        }

        fillAfterPackageAndConfig(taskEvent, nnProxyDeployNodeExtParams, componentNodeDetail);

        fillBeforePackageInfo(taskEvent, nnProxyDeployNodeExtParams, componentId, clusterId, componentNodeDetail.getPackageDiskVersion());

        fillBeforeConfigInfo(taskEvent, nnProxyDeployNodeExtParams, componentId, clusterId, componentNodeDetail.getConfigDiskVersion());

        String dns = componentNodeDetail.getDns();
        if (StringUtils.isBlank(dns)) {
            String errorMsg = String.format("该节点的dns为空,集群id%s,组件id%s", clusterId, componentId);
            throw new RuntimeException(errorMsg);
        }
        if (!Constants.DNS_PATTERN.matcher(dns).matches()) {
            String errorMsg = String.format("该节点的dns格式不正确,%s", dns);
            throw new RuntimeException(errorMsg);
        }


        logPersist(taskEvent, String.format("该节点的dns为%s", dns));
        nnProxyDeployNodeExtParams.setDnsHost(dns);
        nnProxyDeployNodeExtParams.setStartTime(LocalDateTime.now());
        MetadataComponentData componentData = bmrMetadataService.queryComponentByComponentId(componentId);
        nnProxyDeployNodeExtParams.setPriority(componentData.getPriority());
        nnProxyDeployNodeExtParams.setFilled(true);

        executionNodePropsService.saveNodeProp(nodeId, nnProxyDeployNodeExtParams);
        logPersist(taskEvent, "检查并更新任务属性成功");
    }

    private void fillBeforeConfigInfo(TaskEvent taskEvent, NNProxyDeployNodeExtParams nnProxyDeployNodeExtParams, long componentId, Long clusterId, String beforeConfigDiskVersion) {
        List<ComponentConfigVersionEntity> configVersionList = bmrConfigService.queryComponentConfigVersionList(componentId, beforeConfigDiskVersion, 1, 1);
        if (CollectionUtils.isEmpty(configVersionList)) {
            String error = String.format("无法查询到该节点的组件下配置信息,集群id%s,组件id%s,配置版本号%s", clusterId, componentId, beforeConfigDiskVersion);
            logPersist(taskEvent, error);
            throw new RuntimeException(error);
        }
        ComponentConfigVersionEntity beforeConfig = configVersionList.get(0);
        nnProxyDeployNodeExtParams.setBeforeConfigVersion(beforeConfigDiskVersion);
        nnProxyDeployNodeExtParams.setBeforeConfigId(beforeConfig.getId());
    }

    private void fillBeforePackageInfo(TaskEvent taskEvent, NNProxyDeployNodeExtParams nnProxyDeployNodeExtParams, long componentId, Long clusterId, String beforePackageDiskVersion) {
        QueryInstallationPackageListReq req = new QueryInstallationPackageListReq();
        req.setComponentId(componentId);
        req.setTagName(beforePackageDiskVersion);
        req.setPageNum(1);
        req.setPageSize(1);
        List<InstallationPackage> installationPackageList = bmrMetadataService.queryInstallationPackageList(req);
        if (CollectionUtils.isEmpty(installationPackageList)) {
            String error = String.format("无法查询到该节点的组件下安装包信息,集群id%s,组件id%s,安装包名%s", clusterId, componentId, beforePackageDiskVersion);
            throw new RuntimeException(error);
        }
        InstallationPackage beforePackage = installationPackageList.get(0);
        nnProxyDeployNodeExtParams.setBeforePackageVersion(beforePackageDiskVersion);
        nnProxyDeployNodeExtParams.setBeforePackageId(beforePackage.getId());
    }

    private void fillAfterPackageAndConfig(TaskEvent taskEvent, NNProxyDeployNodeExtParams nnProxyDeployNodeExtParams, ComponentNodeDetail componentNodeDetail) {
        boolean packageDeploy = nnProxyDeployNodeExtParams.isContainPackage();
        if (packageDeploy) {
            MetadataPackageData metadataPackage = bmrMetadataService.queryPackageDetailById(nnProxyDeployNodeExtParams.getPackageId());
            Assert.notNull(metadataPackage, String.format("无法查询到该节点的组件%s下安装包信息,安装包id为%s", componentNodeDetail.getComponentName(), nnProxyDeployNodeExtParams.getPackageId()));
            String packageName = metadataPackage.getTagName();
            logPersist(taskEvent, "安装包名称: " + packageName);
            nnProxyDeployNodeExtParams.setPackageVersion(packageName);
        }

        boolean configDeploy = nnProxyDeployNodeExtParams.isContainConfig();
        if (configDeploy) {
            ConfigDetailData configDetailData = bmrConfigService.queryConfigDetailById(nnProxyDeployNodeExtParams.getConfigId());
            Assert.notNull(configDetailData, String.format("无法查询到该节点的组件%s下配置信息,配置id为%s", componentNodeDetail.getComponentName(), nnProxyDeployNodeExtParams.getConfigId()));
            String configVersionNumber = configDetailData.getConfigVersionNumber();
            logPersist(taskEvent, "配置版本号: " + configVersionNumber);
            nnProxyDeployNodeExtParams.setConfigVersion(configVersionNumber);
        }
    }

    /**
     * 查询节点的组件详情
     *
     * @param executionNode
     * @param componentId
     * @param clusterId
     * @return
     */
    private ComponentNodeDetail queryNodeComponentDetail(ExecutionNodeEntity executionNode, long componentId, Long clusterId) {
        QueryComponentNodeListReq queryComponentNodeListReq = new QueryComponentNodeListReq();
        queryComponentNodeListReq.setClusterId(clusterId);
        queryComponentNodeListReq.setComponentId(componentId);
        queryComponentNodeListReq.setApplicationState(Constants.EMPTY_STRING);
        queryComponentNodeListReq.setHostNameList(Arrays.asList(executionNode.getNodeName()));
        queryComponentNodeListReq.setPageNum(1);
        queryComponentNodeListReq.setPageSize(1L);
        queryComponentNodeListReq.setNeedDns(true);
        List<ComponentNodeDetail> componentNodeList = bmrResourceService.queryNodeList(queryComponentNodeListReq);
        if (CollectionUtils.isEmpty(componentNodeList)) {
            return null;
        }
        return componentNodeList.get(0);
    }

    @Override
    public EventTypeEnum getHandleEventType() {
        return EventTypeEnum.NN_PROXY_ITERATION_PRE_CHECK;
    }
}
