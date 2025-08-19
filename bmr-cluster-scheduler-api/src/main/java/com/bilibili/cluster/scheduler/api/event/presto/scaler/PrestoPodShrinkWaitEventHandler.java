package com.bilibili.cluster.scheduler.api.event.presto.scaler;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.json.JSONUtil;
import com.bilibili.cluster.scheduler.api.event.presto.AbstractTidePrestoWaitEventHandler;
import com.bilibili.cluster.scheduler.api.service.bmr.resourceV2.BmrResourceV2Service;
import com.bilibili.cluster.scheduler.common.Constants;
import com.bilibili.cluster.scheduler.common.dto.bmr.resourceV2.TideNodeDetail;
import com.bilibili.cluster.scheduler.common.dto.caster.PodInfo;
import com.bilibili.cluster.scheduler.common.dto.caster.ResourceNodeInfo;
import com.bilibili.cluster.scheduler.common.dto.presto.scaler.PrestoFastScalerExtFlowParams;
import com.bilibili.cluster.scheduler.common.dto.tide.type.DynamicScalingStrategy;
import com.bilibili.cluster.scheduler.common.entity.ExecutionFlowEntity;
import com.bilibili.cluster.scheduler.common.enums.event.EventTypeEnum;
import com.bilibili.cluster.scheduler.common.enums.flow.FlowDeployType;
import com.bilibili.cluster.scheduler.common.enums.resourceV2.TideClusterType;
import com.bilibili.cluster.scheduler.common.enums.resourceV2.TideNodeStatus;
import com.bilibili.cluster.scheduler.common.event.TaskEvent;
import com.bilibili.cluster.scheduler.common.utils.NumberUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Component
public class PrestoPodShrinkWaitEventHandler extends AbstractTidePrestoWaitEventHandler {

    // 是否开启节点预抢占
    @Value("${presto.fast.scaler.preTaint:true}")
    boolean setupPreTaint;

    @Value("${presto.fast.scaler.taint.threshold:0.6}")
    double taintThreshold;

    @Resource
    BmrResourceV2Service resourceV2Service;

    @Override
    protected boolean checkPodCount(TaskEvent taskEvent, List<PodInfo> workPodList) {
        // 检查所有pod状态
        Long flowId = taskEvent.getFlowId();
        PrestoFastScalerExtFlowParams prestoTideExtFlowParams = executionFlowPropsService.getFlowExtParamsByCache(flowId, PrestoFastScalerExtFlowParams.class);

        int lowPodNum = prestoTideExtFlowParams.getLowPodNum();
        logPersist(taskEvent, String.format("当前running的work容器数量为%s,所需容器数量为%s", workPodList.size(), lowPodNum));

        if (workPodList.size() <= lowPodNum) {
            // 遍历节点，对空闲的整机添加污点调度，最大比例 [taintThreshold]
            logPersist(taskEvent, "presto cluster shrink already ok....");
            if (setupPreTaint) {
                final DynamicScalingStrategy dynamicScalingStrategy = prestoTideExtFlowParams.getDynamicScalingStrategy();
                if (dynamicScalingStrategy == DynamicScalingStrategy.FIRST_EXPAND_THEN_SHRINK) {
                    return true;
                }
                logPersist(taskEvent, "presto cluster fast shrink preTaint is 'ON'");
                long yarnClusterId = resourceV2Service.queryCurrentYarnTideClusterId(TideClusterType.PRESTO);
                if (NumberUtils.isPositiveLong(yarnClusterId)) {
                    List<String> preTaintNodeList = preTaintIdleNodes(yarnClusterId, prestoTideExtFlowParams.getHighPodNum(), lowPodNum, taintThreshold, taskEvent);
                    logPersist(taskEvent, "already preTaint node list is : " + JSONUtil.toJsonStr(preTaintNodeList));
                } else {
                    logPersist(taskEvent, "not find presto tide yarn cluster id ......");
                }
            } else {
                logPersist(taskEvent, "presto cluster fast shrink preTaint is 'OFF'");
            }
            return true;
        }
        return false;
    }

    private List<String> preTaintIdleNodes(long yarnClusterId, int highPodNum, int lowPodNum, double taintThreshold, TaskEvent taskEvent) {
        List<String> preTaintNodeList = new ArrayList<>();
        try {
            Double expectPod = Math.floor((highPodNum - lowPodNum) * taintThreshold);
            int maxPreTaintPodNum = expectPod.intValue();
            final List<ResourceNodeInfo> casterNodeList = comCasterService.queryAllNodeInfo(prestoService.getPrestoCasterClusterId());

            String appId = String.valueOf(yarnClusterId);
            final List<TideNodeDetail> tideNodeDetails = resourceV2Service.queryTideOnBizUsedNodes(appId, TideNodeStatus.STAIN, TideClusterType.PRESTO);
            // 查询已经处于污点状态的节点信息
            Map<String, TideNodeDetail> alreadyTaintNodeMap = new HashMap<>();
            if (!CollectionUtils.isEmpty(tideNodeDetails)) {
                for (TideNodeDetail tideNodeDetail : tideNodeDetails) {
                    alreadyTaintNodeMap.put(tideNodeDetail.getHostName(), tideNodeDetail);
                }
            }

            int currentMarkPodNum = 0;

            for (ResourceNodeInfo resourceNodeInfo : casterNodeList) {
                String hostName = resourceNodeInfo.getHostname();
                if (alreadyTaintNodeMap.containsKey(hostName)) {
                    continue;
                }
                final Map<String, String> labels = resourceNodeInfo.getLabels();
                if (CollectionUtil.isEmpty(labels)) {
                    continue;
                }
                final String poolName = labels.getOrDefault("pool", "");
                if (!Constants.TRINO_POOL_NAME.equalsIgnoreCase(poolName)) {
                    log.info("skip pool name {} and host name {} use", poolName, hostName);
                    continue;
                }
                if (resourceNodeInfo.isUnSchedulable()) {
                    continue;
                }

                final TideNodeDetail tideNodeDetail = globalService.getBmrResourceV2Service().queryTideNodeDetail(hostName);
                if (Objects.isNull(tideNodeDetail)) {
                    log.error("hostname={} should sync in bmr resource, but not find.... ", hostName);
                    continue;
                }
                boolean isAvailableNode = judgeIsAvailableIdleNode(resourceNodeInfo, appId, tideNodeDetail);
                if (isAvailableNode) {
                    currentMarkPodNum += transferToPodNum(tideNodeDetail);
                    preTaintNodeList.add(hostName);
                }
                if (currentMarkPodNum >= maxPreTaintPodNum) {
                    break;
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            logPersist(taskEvent, "do pre-taint idle nodes occur error, detail is: " + e.getMessage());
        }
        return preTaintNodeList;
    }

    public boolean judgeIsAvailableIdleNode(ResourceNodeInfo resourceNodeInfo, String appId, TideNodeDetail tideNodeDetail) {
        try {
            final String hostname = resourceNodeInfo.getHostname();
            final String ip = resourceNodeInfo.getName();
            List<PodInfo> currentPodInfoList = comCasterService.queryPodListByNodeIp(prestoService.getPrestoCasterClusterId(), ip);
            return isAvailableNode(currentPodInfoList, hostname, appId, tideNodeDetail);
        } catch (Exception e) {
            return false;
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
        if (org.springframework.util.CollectionUtils.isEmpty(currentPodInfoList)) {
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
            if (org.springframework.util.CollectionUtils.isEmpty(labels)) {
                continue;
            }
            final String appId = labels.getOrDefault("app_id", "");
            if (appId.contains("bmr-trino")) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected boolean checkAllPodState(ExecutionFlowEntity executionFlow) {
        final Long flowId = executionFlow.getId();
        final PrestoFastScalerExtFlowParams extFlowParams = executionFlowPropsService.getFlowExtParamsByCache(flowId, PrestoFastScalerExtFlowParams.class);
        final DynamicScalingStrategy dynamicScalingStrategy = extFlowParams.getDynamicScalingStrategy();

        if (dynamicScalingStrategy == DynamicScalingStrategy.FIRST_SHRINK_THEN_EXPAND) {
            return true;
        }
        return false;
    }

    @Override
    public EventTypeEnum getHandleEventType() {
        return EventTypeEnum.PRESTO_POD_SHRINKAGE_WAIT_READY;
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
