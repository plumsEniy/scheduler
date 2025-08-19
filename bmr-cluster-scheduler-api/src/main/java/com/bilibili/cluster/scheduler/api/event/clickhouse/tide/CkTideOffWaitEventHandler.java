package com.bilibili.cluster.scheduler.api.event.clickhouse.tide;

import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.json.JSONUtil;
import com.bilibili.cluster.scheduler.api.service.GlobalService;
import com.bilibili.cluster.scheduler.api.service.flow.ExecutionFlowPropsService;
import com.bilibili.cluster.scheduler.api.service.flow.prepare.factory.clickhouse.CkTideDeployFlowPrepareGenerateFactory;
import com.bilibili.cluster.scheduler.api.service.tide.TideDynamicNodeGenerateService;
import com.bilibili.cluster.scheduler.common.Constants;
import com.bilibili.cluster.scheduler.common.dto.bmr.resourceV2.TideNodeDetail;
import com.bilibili.cluster.scheduler.common.dto.caster.PodInfo;
import com.bilibili.cluster.scheduler.common.dto.caster.ResourceNodeInfo;
import com.bilibili.cluster.scheduler.common.dto.clickhouse.clickhouse.ClickhouseDeployDTO;
import com.bilibili.cluster.scheduler.common.dto.clickhouse.clickhouse.params.CkTideExtFlowParams;
import com.bilibili.cluster.scheduler.common.dto.clickhouse.clickhouse.templates.template.TemplatePodTemplate;
import com.bilibili.cluster.scheduler.common.dto.flow.prop.BaseFlowExtPropDTO;
import com.bilibili.cluster.scheduler.common.dto.tide.resource.CkTideResource;
import com.bilibili.cluster.scheduler.common.entity.ExecutionNodeEntity;
import com.bilibili.cluster.scheduler.common.enums.event.EventTypeEnum;
import com.bilibili.cluster.scheduler.common.enums.resourceV2.TideClusterType;
import com.bilibili.cluster.scheduler.common.enums.resourceV2.TideNodeStatus;
import com.bilibili.cluster.scheduler.common.event.TaskEvent;
import com.google.common.base.Preconditions;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.apache.http.util.Asserts;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.*;

/**
 * @description: ck下线等待事件
 * @Date: 2024/12/4 20:01
 * @Author: nizhiqiang
 */

@Slf4j
@Component
public class CkTideOffWaitEventHandler extends AbstractCkTideWaitEventHandler {

    @Resource
    ExecutionFlowPropsService flowPropsService;

    @Resource
    TideDynamicNodeGenerateService tideDynamicNodeGenerateService;

    @Resource
    GlobalService globalService;

    @Resource
    CkTideDeployFlowPrepareGenerateFactory ckTideDeployFlowPrepareGenerateFactory;

    private final static Double FINE_RESOURCE_REQUIRE_PERCENT = 0.92d;

    private final static Double MIN_RESOURCE_REQUIRE_PERCENT = 0.7d;

    private final static Double LEAST_RESOURCE_REQUIRE_PERCENT = 0.4d;

    @Override
    protected boolean checkPodCount(TaskEvent taskEvent, CkTideExtFlowParams ckTideExtFlowParams, List<PodInfo> runningPodList) {
        int remainPod = ckTideExtFlowParams.getActualRemainPodCount();
        logPersist(taskEvent, String.format("当前running的pod容器数量为%s,所需容器数量为%s", runningPodList.size(), remainPod));

        if (runningPodList.size() <= remainPod) {
            return true;
        }
        return false;
    }

    @Override
    public EventTypeEnum getHandleEventType() {
        return EventTypeEnum.CK_TIDE_OFF_WAIT_AVAILABLE_NODES;
    }

    /**
     * 等待ck缩容节点可用,仅在阶段一执行
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

    public boolean executeTaskEvent(TaskEvent taskEvent) throws Exception {
        boolean isSuccess = super.executeTaskEvent(taskEvent);

        if (isSuccess) {
            logPersist(taskEvent, "ck cluster shrink ok, start prepareStage2AvailableNodes...");
            isSuccess = prepareStage2AvailableNodes(taskEvent);
            logPersist(taskEvent, "prepareStage2AvailableNodes final status is: " + isSuccess);
        }
        return isSuccess;
    }


    private boolean prepareStage2AvailableNodes(TaskEvent taskEvent) {
        int currTime = 0;
        int maxTime = 10;
        boolean already = false;
        int maxRetry = 3;
        int curRetry = 0;
        while (true) {
            logPersist(taskEvent, "准备节点阶段，当前第【" + currTime + "】次检查空闲节点");
            try {
                already = checkAndPrepareNodes(taskEvent, false);
                if (already) {
                    break;
                }
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                logPersist(taskEvent, "准备和打标空闲节点阶段处理失败，原因：" + e.getMessage());
                if (curRetry > maxRetry) {
                    logPersist(taskEvent, "准备节点阶段超过最大重试次数【" + maxRetry + "】，任务失败。");
                }
                curRetry++;
            }

            ThreadUtil.safeSleep(Constants.ONE_MINUTES);
            if (currTime++ > maxTime) {
                return checkAndPrepareNodes(taskEvent, true);
            }
        }
        return true;
    }

    private boolean checkAndPrepareNodes(TaskEvent taskEvent, boolean isLatestTimeCheck) {
        // 是否达成预期数量

        // 查询当前已经处于污点中的节点列表
        final Long flowId = taskEvent.getFlowId();
        final CkTideExtFlowParams ckTideExtFlowParams = flowPropsService.getFlowExtParamsByCache(flowId, CkTideExtFlowParams.class);
        String appId = ckTideExtFlowParams.getAppId();

        final int remainPod = ckTideExtFlowParams.getRemainPod();
        final int currentPod = ckTideExtFlowParams.getCurrentPod();
        int actualTidePodCount = ckTideExtFlowParams.getActualTidePodCount();
        int expectedShrinkPod = currentPod - remainPod;
        logPersist(taskEvent, String.format("当前实际潮汐pod数量为%s,原预期潮汐pod数量为%s", actualTidePodCount, expectedShrinkPod));

//        预期缩容的pod数

        TemplatePodTemplate ckStableTemplate = getCkStableTemplate(ckTideExtFlowParams);
//        容器所需最小的cpu和内存
        Double cpuReq = ckStableTemplate.getResources().getCpuReq() / 1000.0;
        Integer memReq = ckStableTemplate.getResources().getMemReq() / 1024;

        CkTideResource ckTideResource = new CkTideResource(actualTidePodCount, FINE_RESOURCE_REQUIRE_PERCENT, MIN_RESOURCE_REQUIRE_PERCENT, LEAST_RESOURCE_REQUIRE_PERCENT, cpuReq, memReq);

        logPersist(taskEvent, String.format("need memory is %s, need cpu is %s, memory 单位G，cpu 单位核",
                ckTideResource.getExpectedMemory(), ckTideResource.getExpectedCpu()));

        Map<String, TideNodeDetail> availableNodeMap = new HashMap<>();
        boolean isPrepareReady = checkAvaliableNode(taskEvent, ckTideResource, availableNodeMap, appId);

        if (!isPrepareReady) {
            isPrepareReady = checkComResource(taskEvent, appId, ckTideResource, availableNodeMap);
        }
        if (!isPrepareReady) {
            if (isLatestTimeCheck) {
                isPrepareReady = lastTimeCheck(taskEvent, ckTideResource);
            }
        }

        logPersist(taskEvent, String.format("current memory is %s, current cpu is %s, memory 单位G，cpu 单位核",
                ckTideResource.getCurrentMemory(), ckTideResource.getCurrentCpu()));

        boolean generateNode = ckTideExtFlowParams.isGenerateNode();
//        todo:最后一次的时候生产节点
//        if (isPrepareReady && !generateNode) {
        if (!generateNode && (isPrepareReady || isLatestTimeCheck)) {
            try {
                boolean result = tideDynamicNodeGenerateService.generateTideStage2NodeAndEvents(new ArrayList<>(availableNodeMap.values()), flowId, ckTideDeployFlowPrepareGenerateFactory);
                if (result) {
                    ckTideExtFlowParams.setGenerateNode(true);
                    BaseFlowExtPropDTO baseFlowExtPropDTO = executionFlowPropsService.getFlowPropByFlowId(flowId, BaseFlowExtPropDTO.class);
                    baseFlowExtPropDTO.setFlowExtParams(JSONUtil.toJsonStr(ckTideExtFlowParams));
                    executionFlowPropsService.saveFlowProp(flowId, baseFlowExtPropDTO);
                }
                return true;
//        todo:现阶段没有主机不告警，所以最后一次设为正常执行
//                return result;

            } catch (Exception e) {
                log.error("generateStage2NodeAndEvents error,error is %s", e);
                logPersist(taskEvent, "generateStage2NodeAndEvents error, case by: " + e.getMessage());
                return false;
            }
        }
//        todo:现阶段没有主机不告警，所以最后一次设为正常执行
        if (isLatestTimeCheck) {
            return true;
        }
        return false;
    }

    /**
     * 最后一次检查，如果已经满足预期，则直接返回true
     * 最后一次检查的指标会比之前更低
     *
     * @param taskEvent
     * @param ckTideResource
     * @return
     */
    private boolean lastTimeCheck(TaskEvent taskEvent, CkTideResource ckTideResource) {
        boolean isPrepareReady = false;
        double minCpu = ckTideResource.getMinCpu();
        int minMemory = ckTideResource.getMinMemory();
        String minMessage = String.format("min memory is %s, min cpu is %s.", minMemory, minCpu);
        log.info(minMessage);
        logPersist(taskEvent, minMessage);
        if (ckTideResource.isMin()) {
            isPrepareReady = true;
        } else {
            double leastCpu = ckTideResource.getLeastCpu();
            int leastMemory = ckTideResource.getLeastMemory();
            String leastMessage = String.format("least memory is %s, min cpu is %s.", leastMemory, leastCpu);

            logPersist(taskEvent, leastMessage);
            if (ckTideResource.isLeast()) {
                isPrepareReady = true;
            }
        }
        return isPrepareReady;
    }

    /**
     * 检查并准备COM平台的资源节点，确保满足任务事件的资源需求。
     *
     * @param taskEvent        任务事件对象，包含任务的相关信息。
     * @param appId            应用程序的唯一标识符，用于过滤资源节点。
     * @param ckTideResource   资源对象，用于记录和管理CPU和内存资源。
     * @param availableNodeMap 可用节点映射，用于存储符合条件的节点信息。
     * @return 返回一个布尔值，表示是否成功准备足够的资源节点。
     */
    private boolean checkComResource(TaskEvent taskEvent, String appId, CkTideResource ckTideResource, Map<String, TideNodeDetail> availableNodeMap) {
        boolean isPrepareReady = false;
        // 获取资源池节点信息
        List<ResourceNodeInfo> nodeInfoList = comCasterService.queryAllNodeInfo(Constants.CK_K8S_CLUSTER_ID);

        Preconditions.checkState(!CollectionUtils.isEmpty(nodeInfoList), "ck cluster node list is empty.");

        for (ResourceNodeInfo resourceNodeInfo : nodeInfoList) {
            String hostname = resourceNodeInfo.getHostname();
            final Map<String, String> labels = resourceNodeInfo.getLabels();

            // 如果节点没有标签，跳过该节点
            if (CollectionUtils.isEmpty(labels)) {
                continue;
            }

            // 检查节点是否属于指定的资源池，如果不是则跳过
            final String poolName = labels.getOrDefault("pool", "");
            if (!Constants.CK_ON_ICEBERG_POOL_NAME.equalsIgnoreCase(poolName)) {
                log.info("skip pool name {} and host name {} use", poolName, hostname);
                continue;
            }

            // 如果节点不可调度，则从可用节点映射中移除，并减少资源计数
            String nodeName = resourceNodeInfo.getName();
            if (resourceNodeInfo.isUnSchedulable()) {
                if (availableNodeMap.containsKey(hostname)) {
                    TideNodeDetail nodeDetail = availableNodeMap.remove(hostname);
                    ckTideResource.subMemory(nodeDetail.getMemory());
                    ckTideResource.subCpu(nodeDetail.getCoreNum());
                }
                continue;
            }

            // 如果节点已经在可用节点映射中，跳过该节点
            if (availableNodeMap.containsKey(hostname)) {
                continue;
            }

            List<PodInfo> currentPodInfoList = comCasterService.queryPodListByNodeIp(Constants.CK_K8S_CLUSTER_ID, nodeName);
            final TideNodeDetail tideNodeDetail = globalService.getBmrResourceV2Service().queryTideNodeDetail(hostname);

            boolean available = isAvailableNode(currentPodInfoList, hostname, appId, tideNodeDetail);
            if (!available) {
                continue;
            }
            log.info("ck tide offline available taint node is {} ", hostname);
            // logPersist(taskEvent, "ck tide offline available taint node is: " + hostname);
            logPersist(taskEvent, "更新com平台空闲资源节点,开启污点调度,节点名称: " + hostname);
            availableNodeMap.put(hostname, tideNodeDetail);

            ckTideResource.addCpu(tideNodeDetail.getCoreNum());
            ckTideResource.addMemory(tideNodeDetail.getMemory());

            // 已达成预期节点数量
            if (ckTideResource.isAlready() || ckTideResource.isFine()) {
                isPrepareReady = true;
                break;
            }
        }
        return isPrepareReady;
    }

    /**
     * 检查可用节点并更新资源状态。
     * <p>
     * 遍历初始资源管理系统的可用节点列表，将节点设置为不可调度状态（污点），并更新资源信息。
     * 如果资源达到预期或良好状态，则返回 true，否则返回 false。
     *
     * @param taskEvent             任务事件对象，用于记录日志。
     * @param ckTideResource        资源对象，用于存储和管理 CPU 和内存资源。
     * @param initAvailableNodeList 初始可用节点列表，包含节点的详细信息。
     * @param availableNodeMap      可用节点映射，用于存储已处理的节点信息。
     * @return 如果资源达到预期或良好状态，返回 true；否则返回 false。
     */
    private boolean checkAvaliableNode(TaskEvent taskEvent, CkTideResource ckTideResource, Map<String, TideNodeDetail> availableNodeMap, String appId) {
        boolean isPrepareReady = false;
        List<TideNodeDetail> initAvailableNodeList = globalService.getBmrResourceV2Service().queryTideOnBizUsedNodes(appId, TideNodeStatus.STAIN, TideClusterType.CLICKHOUSE);
        if (!CollectionUtils.isEmpty(initAvailableNodeList)) {
            for (TideNodeDetail nodeDetail : initAvailableNodeList) {
//                节点设为污点
                boolean isSuc = comCasterService.updateNodeToTaintOn(Constants.CK_K8S_CLUSTER_ID, nodeDetail.getIp(), TideClusterType.CLICKHOUSE);
                if (!isSuc) {
                    logPersist(taskEvent, "设置节点不可调度失败,节点名称: " + nodeDetail.getHostName());
                    continue;
                }
                logPersist(taskEvent, "will use already taint node on yarn expansion: " + nodeDetail.getHostName());
                final int coreNum = nodeDetail.getCoreNum();
                final String memory = nodeDetail.getMemory();
//                累加cpu和memory资源
                ckTideResource.addCpu(coreNum);
                ckTideResource.addMemory(memory);
                availableNodeMap.put(nodeDetail.getHostName(), nodeDetail);
            }
            // 已达成预期资源数
            if (ckTideResource.isAlready()) {
                isPrepareReady = true;
            } else {
                // 已达成良好的缩容节点数量
                if (ckTideResource.isFine()) {
                    isPrepareReady = true;
                }
            }
        }
        return isPrepareReady;
    }

    @NotNull
    private TemplatePodTemplate getCkStableTemplate(CkTideExtFlowParams ckTideExtFlowParams) {
        long configId = ckTideExtFlowParams.getConfigId();
        ClickhouseDeployDTO clickhouseDeployDTO = clickhouseService.buildClickhouseDeployDTO(configId);
        List<TemplatePodTemplate> podTemplateList = clickhouseDeployDTO.getChTemplate().getPodTemplates();
        TemplatePodTemplate ckStableTemplate = podTemplateList.stream()
                .filter(podTemplate -> podTemplate.getName().equals(Constants.CK_STABLE_TEMPLATE))
                .findFirst().orElse(null);
        Asserts.notNull(ckStableTemplate, "clickhouse stable pod template is null");
        return ckStableTemplate;
    }

    /**
     * 节点不能是包含keeper或者ck容器
     *
     * @param currentPodInfoList
     * @param hostname
     * @param appId
     * @param tideNodeDetail
     * @return
     */
    private boolean isAvailableNode(List<PodInfo> currentPodInfoList, String hostname, String appId, TideNodeDetail tideNodeDetail) {
        // 节点还未同步？
        if (Objects.isNull(tideNodeDetail)) {
            log.error("query hostname={} not find by bmr resource.", hostname);
            return false;
        }
        final TideNodeStatus casterStatus = tideNodeDetail.getCasterStatus();
        final String currentAppId = tideNodeDetail.getAppId();
        switch (casterStatus) {
            case STAIN:
                if (appId.equals(currentAppId)) {
                    return true;
                } else {
                    return false;
                }
        }

        boolean hasCkPod = hasCkPod(currentPodInfoList);
        if (hasCkPod) {
            return false;
        }
        // 设置污点状态
        boolean taintState = comCasterService.updateNodeToTaintOn(Constants.CK_K8S_CLUSTER_ID, tideNodeDetail.getIp(), TideClusterType.CLICKHOUSE);
        if (!taintState) {
            return false;
        }
        // bmr更新节点占用状态（appId）
        return globalService.getBmrResourceV2Service().updateTideNodeServiceAndStatus(hostname, TideNodeStatus.STAIN, appId, "", TideClusterType.CLICKHOUSE);
    }

    /**
     * 是否包含ck或者keeper容器
     *
     * @param currentPodInfoList
     * @return
     */
    private boolean hasCkPod(List<PodInfo> currentPodInfoList) {
        if (CollectionUtils.isEmpty(currentPodInfoList)) {
            return false;
        }

        for (PodInfo podInfo : currentPodInfoList) {
            final String name = podInfo.getName();
            if (StringUtils.isBlank(name)) {
                continue;
            }
            if (name.contains("keeper")) {
                return true;
            }
            final Map<String, String> labels = podInfo.getLabels();
            if (CollectionUtils.isEmpty(labels)) {
                continue;
            }
            final String scene = labels.getOrDefault("scene", "");
            if (scene.equalsIgnoreCase("clickhouse") || scene.equalsIgnoreCase("clickhouse-keeper")) {
                return true;
            }
        }
        return false;
    }

}
