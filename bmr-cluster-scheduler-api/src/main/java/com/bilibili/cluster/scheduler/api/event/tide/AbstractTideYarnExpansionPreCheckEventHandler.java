package com.bilibili.cluster.scheduler.api.event.tide;

import cn.hutool.core.collection.CollectionUtil;
import com.bilibili.cluster.scheduler.api.event.AbstractTaskEventHandler;
import com.bilibili.cluster.scheduler.api.service.GlobalService;
import com.bilibili.cluster.scheduler.api.service.bmr.metadata.BmrMetadataService;
import com.bilibili.cluster.scheduler.api.service.caster.ComCasterService;
import com.bilibili.cluster.scheduler.api.service.flow.ExecutionFlowPropsService;
import com.bilibili.cluster.scheduler.common.Constants;
import com.bilibili.cluster.scheduler.common.dto.bmr.metadata.MetadataClusterData;
import com.bilibili.cluster.scheduler.common.dto.caster.PodInfo;
import com.bilibili.cluster.scheduler.common.dto.tide.conf.TideConfDTO;
import com.bilibili.cluster.scheduler.common.dto.tide.flow.TideExtFlowParams;
import com.bilibili.cluster.scheduler.common.entity.ExecutionNodeEntity;
import com.bilibili.cluster.scheduler.common.enums.presto.PodType;
import com.bilibili.cluster.scheduler.common.enums.resourceV2.TideClusterType;
import com.bilibili.cluster.scheduler.common.event.TaskEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public abstract class AbstractTideYarnExpansionPreCheckEventHandler extends AbstractTaskEventHandler {

    @Resource
    GlobalService globalService;

    @Resource
    ExecutionFlowPropsService flowPropsService;

    @Resource
    ComCasterService comCasterService;

    @Resource
    BmrMetadataService metadataService;

    @Override
    public boolean executeTaskEvent(TaskEvent taskEvent) throws Exception {
        final Long flowId = taskEvent.getFlowId();
        final List<TideConfDTO> tideConfDTOList = getRequireCheckTideList();

        if (CollectionUtil.isEmpty(tideConfDTOList)) {
            logPersist(taskEvent, "require check tide conf list is empty, skip");
            return true;
        }
        final double checkThreshold = getCheckThreshold();
        logPersist(taskEvent, "start check tide conf list, checkThreshold is：" + checkThreshold);

        Map<Long, Boolean> componentCheckResult = new HashMap<>();
        int passCnt = 0;
        int allCnt = tideConfDTOList.size();
        int checkTime = 0;
        while (passCnt < allCnt) {
            checkTime++;
            String msg = String.format("start %s time(s) check", checkTime);
            log.info(msg);
            logPersist(taskEvent, msg);
            for (TideConfDTO tideConfDTO : tideConfDTOList) {
                final long componentId = tideConfDTO.getComponentId();
                if (componentCheckResult.containsKey(componentId)) {
                    continue;
                }
                boolean isReady;
                try {
                    isReady = checkPodNumIsReady(tideConfDTO, checkThreshold);
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                    isReady = false;
                    logPersist(taskEvent, tideConfDTO.getAppId() + " check pod num error, case: " + e.getMessage());
                }
                if (isReady) {
                    logPersist(taskEvent, tideConfDTO.getAppId() + " check pod num pass");
                    passCnt++;
                    componentCheckResult.put(componentId, Boolean.TRUE);
                }
            }

            if (passCnt >= allCnt) {
                logPersist(taskEvent, "all check pass");
                return true;
            }
            if (checkTime >= 20) {
                logPersist(taskEvent, "check 20 times, but not pass....");
                return false;
            }
            Thread.sleep(Constants.ONE_MINUTES);
        }
        return false;
    }

    protected boolean checkPodNumIsReady(TideConfDTO tideConfDTO, double checkThreshold) {
        final int highPodNum = tideConfDTO.getHighPodNum();
        final int lowPodNum = tideConfDTO.getLowPodNum();
        Double diffNum = (highPodNum - lowPodNum) * checkThreshold;
        int requirePodNum = lowPodNum + diffNum.intValue();
        if (lowPodNum == requirePodNum) {
            requirePodNum ++;
        }
        if (requirePodNum > highPodNum) {
            requirePodNum = highPodNum;
        }

        final String appId = tideConfDTO.getAppId();
        final long clusterId = tideConfDTO.getClusterId();
        final MetadataClusterData metadataClusterData = metadataService.queryClusterDetail(clusterId);
        String networkEnvironment = metadataClusterData.getNetworkEnvironment().getEnv();
        String podSelector = String.format("app_id=%s,env=%s", appId, networkEnvironment);
        final List<PodInfo> podInfoList = comCasterService.queryPodList(getTideCasterClusterId(), podSelector, null, null);

        List<PodInfo> workPodList = new ArrayList<>();
        for (PodInfo podInfo : podInfoList) {
            PodType podType = PodType.getPodType(podInfo.getName());
            if (!podType.equals(PodType.WORKER)) {
                continue;
            }
            String podStatus = podInfo.getPodStatus();
            if (!Constants.POD_STATUS_SUCCESS.equals(podStatus)) {
                continue;
            }
            workPodList.add(podInfo);
        }

        if (workPodList.size() >= requirePodNum) {
            return true;
        }
        return false;
    }


    protected TideExtFlowParams getTideFlowExtParams(long flowId) {
        return flowPropsService.getFlowExtParamsByCache(flowId, TideExtFlowParams.class);
    }

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

    protected abstract long getTideCasterClusterId();

    protected abstract TideClusterType getTideClusterType();

    protected abstract String getDeployService();

    protected double getCheckThreshold() {
        return 0.8d;
    }

    protected abstract List<TideConfDTO> getRequireCheckTideList();

}
