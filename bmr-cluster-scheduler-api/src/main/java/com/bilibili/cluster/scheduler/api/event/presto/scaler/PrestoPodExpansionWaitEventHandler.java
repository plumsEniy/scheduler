package com.bilibili.cluster.scheduler.api.event.presto.scaler;

import cn.hutool.core.collection.CollectionUtil;
import com.bilibili.cluster.scheduler.api.event.presto.AbstractTidePrestoWaitEventHandler;
import com.bilibili.cluster.scheduler.api.service.bmr.resourceV2.BmrResourceV2Service;
import com.bilibili.cluster.scheduler.common.dto.bmr.resourceV2.TideNodeDetail;
import com.bilibili.cluster.scheduler.common.dto.caster.PodInfo;
import com.bilibili.cluster.scheduler.common.dto.presto.scaler.PrestoFastScalerExtFlowParams;
import com.bilibili.cluster.scheduler.common.dto.tide.type.DynamicScalingStrategy;
import com.bilibili.cluster.scheduler.common.enums.event.EventTypeEnum;
import com.bilibili.cluster.scheduler.common.enums.resourceV2.TideClusterType;
import com.bilibili.cluster.scheduler.common.enums.resourceV2.TideNodeStatus;
import com.bilibili.cluster.scheduler.common.event.TaskEvent;
import com.bilibili.cluster.scheduler.common.utils.NumberUtils;
import com.google.common.base.Preconditions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;

@Slf4j
@Component
public class PrestoPodExpansionWaitEventHandler extends AbstractTidePrestoWaitEventHandler {

    @Resource
    BmrResourceV2Service resourceV2Service;

    @Override
    public EventTypeEnum getHandleEventType() {
        return EventTypeEnum.PRESTO_POD_EXPANSION_WAIT_READY;
    }

    @Override
    protected boolean checkPodCount(TaskEvent taskEvent, List<PodInfo> workPodList) {
        Long flowId = taskEvent.getFlowId();
        PrestoFastScalerExtFlowParams prestoTideExtFlowParams = executionFlowPropsService.getFlowExtParamsByCache(flowId, PrestoFastScalerExtFlowParams.class);

        int lowPodNum = prestoTideExtFlowParams.getLowPodNum();
        int highPodNum = prestoTideExtFlowParams.getHighPodNum();
        double threshold = prestoTideExtFlowParams.getThreshold();
        Double minDiffCount = threshold * (highPodNum - lowPodNum);
        int minPodNum = minDiffCount.intValue() + lowPodNum;

        logPersist(taskEvent, String.format("当前running的work容器数量为%s, threshold=%s,所需容器最少数量为%s",
                workPodList.size(), threshold, minPodNum));

        if (workPodList.size() >= minPodNum) {
            return true;
        }
        return false;
    }

    // 在快速扩缩容场景，开启了污点预抢占，尝试逐步释放资源(仅适用于先扩后缩场景)
    public void releaseTideResourceForExpansion(int checkTime, TaskEvent taskEvent, List<PodInfo> readyPodList) {
        Long flowId = taskEvent.getFlowId();
        final PrestoFastScalerExtFlowParams extFlowParams = executionFlowPropsService.getFlowExtParamsByCache(flowId, PrestoFastScalerExtFlowParams.class);
        final DynamicScalingStrategy dynamicScalingStrategy = extFlowParams.getDynamicScalingStrategy();

        if (dynamicScalingStrategy != DynamicScalingStrategy.FIRST_EXPAND_THEN_SHRINK) {
            return;
        }

        //前三次不释放资源
        if (checkTime <= 3) {
            return;
        }
        // 尝试一次性释放所需资源
        if (checkTime % 2 == 0) {
            PrestoFastScalerExtFlowParams prestoTideExtFlowParams = executionFlowPropsService.getFlowExtParamsByCache(flowId, PrestoFastScalerExtFlowParams.class);

            int highPodNum = prestoTideExtFlowParams.getHighPodNum();
            int currentPodNum = readyPodList.size();

            int requireReleasePodNum = highPodNum - currentPodNum;
            if (requireReleasePodNum <= 0) {
                return;
            }
            tryReleaseTaintResource(taskEvent, requireReleasePodNum);
        }
    }

    private void tryReleaseTaintResource(TaskEvent taskEvent, int requireReleasePodNum) {
        long yarnClusterId = resourceV2Service.queryCurrentYarnTideClusterId(TideClusterType.PRESTO);
        if (!NumberUtils.isPositiveLong(yarnClusterId)) {
            return;
        }
        int releasePodNum = 0;
        String appId = String.valueOf(yarnClusterId);
        final List<TideNodeDetail> tideNodeDetails = resourceV2Service.queryTideOnBizUsedNodes(appId, TideNodeStatus.STAIN, TideClusterType.PRESTO);
        // 查询已经处于污点状态的节点信息
        if (CollectionUtil.isEmpty(tideNodeDetails)) {
            return;
        }

        for (TideNodeDetail nodeDetail : tideNodeDetails) {
            final String hostname = nodeDetail.getHostName();
            try {
                // 释放污点资源，更新节点状态至可调度状态（关闭污点调度）
                boolean isSuc = comCasterService.updateNodeToTaintOff(prestoService.getPrestoCasterClusterId(), nodeDetail.getIp(), TideClusterType.PRESTO);
                Preconditions.checkState(isSuc, "com caster updateNodeToTaintOff failed");
                logPersist(taskEvent, "释放污点资源，更新节点状态至可调度状态（关闭污点调度）完成,节点名称: " + hostname);
                // 更新资源池节点状态至可用状态
                globalService.getBmrResourceV2Service().updateTideNodeServiceAndStatus(
                        hostname, TideNodeStatus.AVAILABLE, "", "presto", TideClusterType.PRESTO);
                releasePodNum += transferToPodNum(nodeDetail);
                if (releasePodNum > requireReleasePodNum) {
                    break;
                }
            } catch (Exception e) {
                log.error("release taint node error, hostname is {}", hostname);
                log.error(e.getMessage(), e);
            }
        }
    }

    protected int transferToPodNum(TideNodeDetail nodeDetail) {
        final int coreNum = nodeDetail.getCoreNum();
        if (coreNum >= 128) {
            return 3;
        } else if (coreNum >= 96) {
            return 2;
        } else {
            return 1;
        }
    }

}
