package com.bilibili.cluster.scheduler.api.event.tide;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.thread.ThreadUtil;
import com.bilibili.cluster.scheduler.api.event.AbstractTaskEventHandler;
import com.bilibili.cluster.scheduler.api.service.caster.ComCasterService;
import com.bilibili.cluster.scheduler.api.service.flow.ExecutionFlowPropsService;
import com.bilibili.cluster.scheduler.api.service.flow.prepare.factory.FlowPrepareGenerateFactory;
import com.bilibili.cluster.scheduler.api.service.tide.TideDynamicNodeGenerateService;
import com.bilibili.cluster.scheduler.common.Constants;
import com.bilibili.cluster.scheduler.common.dto.bmr.resourceV2.TideNodeDetail;
import com.bilibili.cluster.scheduler.common.dto.caster.ResourceNodeInfo;
import com.bilibili.cluster.scheduler.common.dto.tide.flow.TideExtFlowParams;
import com.bilibili.cluster.scheduler.common.entity.ExecutionNodeEntity;
import com.bilibili.cluster.scheduler.common.enums.resourceV2.TideClusterType;
import com.bilibili.cluster.scheduler.common.enums.resourceV2.TideNodeStatus;
import com.bilibili.cluster.scheduler.common.event.TaskEvent;
import com.google.common.base.Preconditions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Component
public abstract class AbstractTideYarnExpansionWaitNodesEventHandler extends AbstractTaskEventHandler {

    @Resource
    protected ExecutionFlowPropsService flowPropsService;

    @Resource
    protected ComCasterService comCasterService;

    @Resource
    TideDynamicNodeGenerateService tideDynamicNodeGenerateService;

    @Override
    public boolean executeTaskEvent(TaskEvent taskEvent) throws Exception {
        // 动态生成2阶段yarn扩容节点
        boolean prepareResult = prepareStage2AvailableNodes(taskEvent);
        return prepareResult;
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

        final Long flowId = taskEvent.getFlowId();
        final TideExtFlowParams tideExtFlowParams = flowPropsService.getFlowExtParamsByCache(flowId, TideExtFlowParams.class);
        // 阶段已经生成完毕
        if (tideExtFlowParams.isGenerateNode()) {
            return true;
        }
        String appId = tideExtFlowParams.getAppId();

        // 计算得到最大的扩容机器等效的pod数量
        Double thresholdPodNum = getMaxThreshold() * getDiffPodCount(flowId);
        int maxExpectedExpansionPodNum = thresholdPodNum.intValue();

        // 当前已经可用的节点信息
        int currentReadyPodNum = 0;
        Map<String, TideNodeDetail> availableNodeMap = new HashMap<>();

        // 查询当前已经处于污点中的节点列表
        List<TideNodeDetail> tideNodeDetails = globalService.getBmrResourceV2Service().queryTideOnBizUsedNodes(appId, TideNodeStatus.STAIN, getTideClusterType());
        if (!CollectionUtil.isEmpty(tideNodeDetails)) {
            for (TideNodeDetail nodeDetail : tideNodeDetails) {
                boolean isSuc = comCasterService.updateNodeToTaintOn(getTideCasterClusterId(), nodeDetail.getIp(), getTideClusterType());
                if (!isSuc) {
                    logPersist(taskEvent, "设置节点不可调度失败,节点名称: " + nodeDetail.getHostName());
                    continue;
                }
                logPersist(taskEvent, "will use already taint node on yarn expansion: " + nodeDetail.getHostName());

                // 等效pod数量
                int equivalencePodNum = transferToPodNum(nodeDetail);
                availableNodeMap.put(nodeDetail.getHostName(), nodeDetail);
                currentReadyPodNum += equivalencePodNum;
            }
        }

        if (currentReadyPodNum >= maxExpectedExpansionPodNum) {
            isPrepareReady = true;
        }

        if (!isPrepareReady) {
            // 获取资源池节点信息
            List<ResourceNodeInfo> nodeInfoList = comCasterService.queryAllNodeInfo(getTideCasterClusterId());
            Preconditions.checkState(!CollectionUtils.isEmpty(nodeInfoList), "query com cluster node list is empty.");

            for (ResourceNodeInfo resourceNodeInfo : nodeInfoList) {
                // 跳过不可调度节点
                if (resourceNodeInfo.isUnSchedulable()) {
                    continue;
                }
                String hostname = resourceNodeInfo.getHostname();
                if (availableNodeMap.containsKey(hostname)) {
                    continue;
                }

                final TideNodeDetail nodeDetail = globalService.getBmrResourceV2Service().queryTideNodeDetail(hostname);
                if (Objects.isNull(nodeDetail)) {
                    log.error("hostname={} should sync in bmr resource, but not find.... ", hostname);
                    continue;
                }

                boolean isAvailableNode = judgeIsAvailableIdleNode(resourceNodeInfo, appId, nodeDetail);
                if (isAvailableNode) {
                    logPersist(taskEvent, "更新com平台空闲资源节点,开启污点调度,节点名称: " + hostname);
                    int equivalencePodNum = transferToPodNum(nodeDetail);
                    availableNodeMap.put(nodeDetail.getHostName(), nodeDetail);
                    currentReadyPodNum += equivalencePodNum;
                    if (currentReadyPodNum >= maxExpectedExpansionPodNum) {
                        isPrepareReady = true;
                        break;
                    }
                }
            }
        }

        if (!isPrepareReady) {
            if (isLatestTimeCheck) {
                logPersist(taskEvent, "lastTimeCheck available pod num is :" + currentReadyPodNum);
                // 最后检查直接置为成功
                isPrepareReady = true;
            }
        }

        if (isPrepareReady) {
            logPersist(taskEvent, "current ready pod num is: " + currentReadyPodNum);
            logPersist(taskEvent, "expected expansion pod num of: " + maxExpectedExpansionPodNum);
            if (availableNodeMap.size() > 0) {
                try {
                    boolean result = tideDynamicNodeGenerateService.generateTideStage2NodeAndEvents(new ArrayList<>(availableNodeMap.values()), flowId, getFlowPrepareGenerateFactory());
                    if (result) {
                        updateStage2NodeGenerated(flowId);
                    }
                    return result;
                } catch (Exception e) {
                    log.error("generateStage2NodeAndEvents error");
                    log.error(e.getMessage(), e);
                    logPersist(taskEvent, "generateStage2NodeAndEvents error, case by: " + e.getMessage());
                    return false;
                }
            } else {
                logPersist(taskEvent, "not find any available idle node.....");
            }
        }

        return isPrepareReady;
    }

    protected abstract boolean judgeIsAvailableIdleNode(ResourceNodeInfo resourceNodeInfo, String appId, TideNodeDetail nodeDetail);

    protected abstract void updateStage2NodeGenerated(long flowId);

    protected abstract long getTideCasterClusterId();

    protected abstract TideClusterType getTideClusterType();

    protected abstract int transferToPodNum(TideNodeDetail nodeDetail);

    protected abstract FlowPrepareGenerateFactory getFlowPrepareGenerateFactory();

    // 最大出借数量的阈值
    protected double getMaxThreshold() {
        return 0.92d;
    }

    // 获取配置的出借Pod数量
    protected abstract int getDiffPodCount(long flowId);

    /**
     * 获取当前com资源池空闲机器节点，执行污点操作，并生成2阶段yarn节点，仅在stage1执行
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
