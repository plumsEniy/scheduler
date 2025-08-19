package com.bilibili.cluster.scheduler.api.event.presto.tide;

import cn.hutool.json.JSONUtil;
import com.bilibili.cluster.scheduler.api.event.tide.AbstractTideYarnExpansionWaitNodesEventHandler;
import com.bilibili.cluster.scheduler.api.service.flow.prepare.factory.FlowPrepareGenerateFactory;
import com.bilibili.cluster.scheduler.api.service.flow.prepare.factory.yarn.YarnTideFlowPrepareGenerateFactory;
import com.bilibili.cluster.scheduler.api.service.presto.PrestoService;
import com.bilibili.cluster.scheduler.common.Constants;
import com.bilibili.cluster.scheduler.common.dto.bmr.resourceV2.TideNodeDetail;
import com.bilibili.cluster.scheduler.common.dto.caster.PodInfo;
import com.bilibili.cluster.scheduler.common.dto.caster.ResourceNodeInfo;
import com.bilibili.cluster.scheduler.common.dto.flow.prop.BaseFlowExtPropDTO;
import com.bilibili.cluster.scheduler.common.dto.tide.flow.YarnTideExtFlowParams;
import com.bilibili.cluster.scheduler.common.enums.event.EventTypeEnum;
import com.bilibili.cluster.scheduler.common.enums.resourceV2.TideClusterType;
import com.bilibili.cluster.scheduler.common.enums.resourceV2.TideNodeStatus;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Component
public class PrestoTideYarnExpansionWaitNodesEventHandler extends AbstractTideYarnExpansionWaitNodesEventHandler {

    @Resource
    PrestoService prestoService;

    @Resource
    YarnTideFlowPrepareGenerateFactory yarnTideFlowPrepareGenerateFactory;

    @Override
    public EventTypeEnum getHandleEventType() {
        return EventTypeEnum.PRESTO_YARN_TIDE_EXPANSION_WAITING_AVAILABLE_NODES;
    }

    @Override
    protected boolean judgeIsAvailableIdleNode(ResourceNodeInfo resourceNodeInfo, String appId, TideNodeDetail nodeDetail) {
        final String hostname = resourceNodeInfo.getHostname();
        final Map<String, String> labels = resourceNodeInfo.getLabels();
        // 需要是trino标签的资源才可参与潮汐
        final String poolName = labels.getOrDefault("pool", "");
        if (!Constants.TRINO_POOL_NAME.equalsIgnoreCase(poolName)) {
            log.info("skip pool name {} and host name {} use", poolName, hostname);
            return false;
        }
        final String ip = resourceNodeInfo.getName();
        List<PodInfo> currentPodInfoList = comCasterService.queryPodListByNodeIp(prestoService.getPrestoCasterClusterId(), ip);
        return isAvailableNode(currentPodInfoList, hostname, appId, nodeDetail);
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

    @Override
    protected void updateStage2NodeGenerated(long flowId) {
        final YarnTideExtFlowParams tideExtFlowParams = flowPropsService.getFlowExtParamsByCache(flowId, YarnTideExtFlowParams.class);
        tideExtFlowParams.setGenerateNode(true);
        final BaseFlowExtPropDTO baseFlowExtPropDTO = new BaseFlowExtPropDTO();
        baseFlowExtPropDTO.setFlowExtParams(JSONUtil.toJsonStr(tideExtFlowParams));
        flowPropsService.saveFlowProp(flowId, baseFlowExtPropDTO);
    }

    @Override
    protected long getTideCasterClusterId() {
        return prestoService.getPrestoCasterClusterId();
    }

    @Override
    protected TideClusterType getTideClusterType() {
        return TideClusterType.PRESTO;
    }

    @Override
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

    @Override
    protected FlowPrepareGenerateFactory getFlowPrepareGenerateFactory() {
        return yarnTideFlowPrepareGenerateFactory;
    }

    @Override
    protected int getDiffPodCount(long flowId) {
        final YarnTideExtFlowParams tideExtFlowParams = flowPropsService.getFlowExtParamsByCache(flowId, YarnTideExtFlowParams.class);
        return tideExtFlowParams.getExpectedCount();

    }
}
