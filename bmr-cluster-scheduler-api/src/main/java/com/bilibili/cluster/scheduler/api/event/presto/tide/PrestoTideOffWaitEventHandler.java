package com.bilibili.cluster.scheduler.api.event.presto.tide;

import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.json.JSONUtil;
import com.bilibili.cluster.scheduler.api.event.presto.AbstractTidePrestoWaitEventHandler;
import com.bilibili.cluster.scheduler.api.service.GlobalService;
import com.bilibili.cluster.scheduler.api.service.flow.ExecutionFlowPropsService;
import com.bilibili.cluster.scheduler.api.service.flow.prepare.factory.presto.PrestoTideDeployFlowPrepareGenerateFactory;
import com.bilibili.cluster.scheduler.api.service.tide.TideDynamicNodeGenerateService;
import com.bilibili.cluster.scheduler.api.service.presto.PrestoService;
import com.bilibili.cluster.scheduler.common.Constants;
import com.bilibili.cluster.scheduler.common.dto.caster.PodInfo;
import com.bilibili.cluster.scheduler.common.dto.caster.ResourceNodeInfo;
import com.bilibili.cluster.scheduler.common.dto.bmr.resourceV2.TideNodeDetail;
import com.bilibili.cluster.scheduler.common.dto.flow.prop.BaseFlowExtPropDTO;
import com.bilibili.cluster.scheduler.common.enums.resourceV2.TideClusterType;
import com.bilibili.cluster.scheduler.common.enums.resourceV2.TideNodeStatus;
import com.bilibili.cluster.scheduler.common.dto.presto.tide.PrestoTideExtFlowParams;
import com.bilibili.cluster.scheduler.common.entity.ExecutionNodeEntity;
import com.bilibili.cluster.scheduler.common.enums.event.EventTypeEnum;
import com.bilibili.cluster.scheduler.common.event.TaskEvent;
import com.google.common.base.Preconditions;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @description: presto下线等待事件
 * @Date: 2024/12/4 20:01
 * @Author: nizhiqiang
 */

@Slf4j
@Component
public class PrestoTideOffWaitEventHandler extends AbstractTidePrestoWaitEventHandler {

    @Resource
    ExecutionFlowPropsService flowPropsService;

    @Resource
    TideDynamicNodeGenerateService tideDynamicNodeGenerateService;

    @Resource
    GlobalService globalService;

    @Resource
    PrestoTideDeployFlowPrepareGenerateFactory prestoTideDeployFlowPrepareGenerateFactory;

    @Resource
    PrestoService prestoService;

    private final static Double FINE_RESOURCE_REQUIRE_PERCENT = 0.92d;

    private final static Double MIN_RESOURCE_REQUIRE_PERCENT = 0.7d;

    private final static Double LEAST_RESOURCE_REQUIRE_PERCENT = 0.4d;

    @Override
    protected boolean checkPodCount(TaskEvent taskEvent, List<PodInfo> workPodList) {
        Long flowId = taskEvent.getFlowId();
        PrestoTideExtFlowParams prestoTideExtFlowParams = executionFlowPropsService.getFlowExtParamsByCache(flowId, PrestoTideExtFlowParams.class);
        int remainPod = prestoTideExtFlowParams.getRemainPod();
        logPersist(taskEvent, String.format("当前running的work容器数量为%s,所需容器数量为%s", workPodList.size(), remainPod));

        if (workPodList.size() <= remainPod) {
            return true;
        }
        return false;
    }

    @Override
    public EventTypeEnum getHandleEventType() {
        return EventTypeEnum.PRESTO_TIDE_OFF_WAIT_AVAILABLE_NODES;
    }

    /**
     * 等待presto缩容节点可用,仅在阶段一执行
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
            logPersist(taskEvent, "presto cluster shrink ok, start prepareStage2AvailableNodes...");
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
                    return false;
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
        boolean isPrepareReady = false;

        // 查询当前已经处于污点中的节点列表
        final Long flowId = taskEvent.getFlowId();
        final PrestoTideExtFlowParams prestoTideExtFlowParams = flowPropsService.getFlowExtParamsByCache(flowId, PrestoTideExtFlowParams.class);
        String appId = prestoTideExtFlowParams.getAppId();

        final int remainPod = prestoTideExtFlowParams.getRemainPod();
        final int currentPod = prestoTideExtFlowParams.getCurrentPod();

        int expectedShrinkPod = currentPod - remainPod;
        int alreadyAvailablePod = 0;

        Double fineShrinkPodValue = Math.floor(expectedShrinkPod * FINE_RESOURCE_REQUIRE_PERCENT);
        int fineShrinkPod = fineShrinkPodValue.intValue();

        List<TideNodeDetail> initAvailableNodeList = globalService.getBmrResourceV2Service().queryTideOnBizUsedNodes(appId, TideNodeStatus.STAIN, TideClusterType.PRESTO);
        Map<String, TideNodeDetail> availableNodeMap = new HashMap<>();
        if (!CollectionUtils.isEmpty(initAvailableNodeList)) {
            for (TideNodeDetail nodeDetail : initAvailableNodeList) {
                boolean isSuc = comCasterService.updateNodeToTaintOn(prestoService.getPrestoCasterClusterId(), nodeDetail.getIp(), TideClusterType.PRESTO);
                if (!isSuc) {
                    logPersist(taskEvent, "设置节点不可调度失败,节点名称: " + nodeDetail.getHostName());
                    continue;
                }
                logPersist(taskEvent, "will use already taint node on yarn expansion: " + nodeDetail.getHostName());
                final int coreNum = nodeDetail.getCoreNum();
                // 等效pod数量
                int equivalencePodNum = transferPodNum(coreNum);
                alreadyAvailablePod += equivalencePodNum;
                availableNodeMap.put(nodeDetail.getHostName(), nodeDetail);
            }
            // 已达成预期节点数量
            if (alreadyAvailablePod >= expectedShrinkPod) {
                isPrepareReady = true;
            } else {
                // 已达成良好的缩容节点数量
                if (alreadyAvailablePod >= fineShrinkPod) {
                    isPrepareReady = true;
                }
            }
        }

        if (!isPrepareReady) {
            // 获取资源池节点信息
            List<ResourceNodeInfo> nodeInfoList = comCasterService.queryAllNodeInfo(prestoService.getPrestoCasterClusterId());
            Preconditions.checkState(!CollectionUtils.isEmpty(nodeInfoList), "presto cluster node list is empty.");

            for (ResourceNodeInfo resourceNodeInfo : nodeInfoList) {
                String hostname = resourceNodeInfo.getHostname();
                final Map<String, String> labels = resourceNodeInfo.getLabels();
                if (CollectionUtils.isEmpty(labels)) {
                    continue;
                }
                final String poolName = labels.getOrDefault("pool", "");
                if (!Constants.TRINO_POOL_NAME.equalsIgnoreCase(poolName)) {
                    log.info("skip pool name {} and host name {} use", poolName, hostname);
                    continue;
                }

                String nodeName = resourceNodeInfo.getName();
                if (resourceNodeInfo.isUnSchedulable()) {
                    if (availableNodeMap.containsKey(hostname)) {
                        TideNodeDetail nodeDetail = availableNodeMap.remove(hostname);
                        int equivalencePodNum = transferPodNum(nodeDetail.getCoreNum());
                        alreadyAvailablePod -= equivalencePodNum;
                    }
                    continue;
                }

                if (availableNodeMap.containsKey(hostname)) {
                    continue;
                }
                List<PodInfo> currentPodInfoList = comCasterService.queryPodListByNodeIp(prestoService.getPrestoCasterClusterId(), nodeName);
                final TideNodeDetail tideNodeDetail = globalService.getBmrResourceV2Service().queryTideNodeDetail(hostname);

                boolean available = isAvailableNode(currentPodInfoList, hostname, appId, tideNodeDetail);
                if (!available) {
                    continue;
                }
                log.info("presto tide offline available taint node is {} ", hostname);
                // logPersist(taskEvent, "presto tide offline available taint node is: " + hostname);
                logPersist(taskEvent, "更新com平台空闲资源节点,开启污点调度,节点名称: " + hostname);
                availableNodeMap.put(hostname, tideNodeDetail);

                final int coreNum = tideNodeDetail.getCoreNum();
                int equivalencePodNum = transferPodNum(coreNum);
                alreadyAvailablePod += equivalencePodNum;
                // 已达成预期节点数量
                if (alreadyAvailablePod >= expectedShrinkPod) {
                    isPrepareReady = true;
                    break;
                }
                // 已达成良好的缩容节点数量
                if (alreadyAvailablePod >= fineShrinkPod) {
                    isPrepareReady = true;
                    break;
                }
            }
        }
        if (!isPrepareReady) {
            if (isLatestTimeCheck) {
                Double minShrinkPodValue = Math.floor(expectedShrinkPod * MIN_RESOURCE_REQUIRE_PERCENT);
                int minShrinkPod = minShrinkPodValue.intValue();
                if (minShrinkPod <= 0) {
                    minShrinkPod = 1;
                }
                log.info("minShrinkPod is {}.", minShrinkPod);
                logPersist(taskEvent, "lastTimeCheck require minShrinkPod is: " + minShrinkPod);
                if (alreadyAvailablePod >= minShrinkPod) {
                    isPrepareReady = true;
                } else {
                    Double leastShrinkPodValue = Math.floor(expectedShrinkPod * LEAST_RESOURCE_REQUIRE_PERCENT);
                    logPersist(taskEvent, "leastTimeCheck require leastShrinkPod is: " + leastShrinkPodValue);
                    int leastShrinkPod = leastShrinkPodValue.intValue();
                    if (leastShrinkPod <= 0) {
                        leastShrinkPod = 1;
                    }
                    log.info("minShrinkPod is {}.", leastShrinkPod);
                    logPersist(taskEvent, "lastTimeCheck require leastShrinkPod is: " + leastShrinkPod);
                    if (alreadyAvailablePod >= leastShrinkPod) {
                        isPrepareReady = true;
                    }
                }
            }
        }

        if (isPrepareReady) {
            logPersist(taskEvent, "available taint node prepare ready. alreadyAvailablePod is ==== " + alreadyAvailablePod);
            try {
                boolean result = tideDynamicNodeGenerateService.generateTideStage2NodeAndEvents(new ArrayList<>(availableNodeMap.values()), flowId, prestoTideDeployFlowPrepareGenerateFactory);
                if (result) {
                    prestoTideExtFlowParams.setGenerateNode(true);
                    BaseFlowExtPropDTO baseFlowExtPropDTO = executionFlowPropsService.getFlowPropByFlowId(flowId, BaseFlowExtPropDTO.class);
                    baseFlowExtPropDTO.setFlowExtParams(JSONUtil.toJsonStr(prestoTideExtFlowParams));
                    executionFlowPropsService.saveFlowProp(flowId, baseFlowExtPropDTO);
                }
                return result;
            } catch (Exception e) {
                log.error("generateStage2NodeAndEvents error");
                log.error(e.getMessage(), e);
                logPersist(taskEvent, "generateStage2NodeAndEvents error, case by: " + e.getMessage());
                return false;
            }
        }
        return false;
    }

    private int transferPodNum(int coreNum) {
        if (coreNum >= 128) {
            return 3;
        } else if (coreNum >= 96) {
            return 2;
        } else {
            return 1;
        }
    }

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

        boolean hasTrinoPod = hasTrinoPod(currentPodInfoList);
        if (hasTrinoPod) {
            return false;
        }
        // 设置污点状态
        boolean taintState = comCasterService.updateNodeToTaintOn(prestoService.getPrestoCasterClusterId(), tideNodeDetail.getIp(), TideClusterType.PRESTO);
        if (!taintState) {
            return false;
        }
        // bmr更新节点占用状态（appId）
        return globalService.getBmrResourceV2Service().updateTideNodeServiceAndStatus(hostname, TideNodeStatus.STAIN, appId, "", TideClusterType.PRESTO);
    }

    private boolean hasTrinoPod(List<PodInfo> currentPodInfoList) {
        if (CollectionUtils.isEmpty(currentPodInfoList)) {
            return false;
        }

        for (PodInfo podInfo : currentPodInfoList) {
            final String name = podInfo.getName();
            if (StringUtils.isBlank(name)) {
                continue;
            }
            if (name.contains("trino")) {
                return true;
            }
            final Map<String, String> labels = podInfo.getLabels();
            if (CollectionUtils.isEmpty(labels)) {
                continue;
            }
            final String appId = labels.getOrDefault("app_id", "");
            if (appId.contains("bmr-trino")) {
                return true;
            }
        }
        return false;
    }

}
