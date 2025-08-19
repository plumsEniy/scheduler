package com.bilibili.cluster.scheduler.api.event.clickhouse.tide;

import com.bilibili.cluster.scheduler.api.event.AbstractTaskEventHandler;
import com.bilibili.cluster.scheduler.api.service.bmr.manager.BmrManagerService;
import com.bilibili.cluster.scheduler.api.service.bmr.metadata.BmrMetadataService;
import com.bilibili.cluster.scheduler.api.service.caster.ComCasterService;
import com.bilibili.cluster.scheduler.api.service.clickhouse.clickhouse.ClickhouseService;
import com.bilibili.cluster.scheduler.common.Constants;
import com.bilibili.cluster.scheduler.common.dto.bmr.metadata.MetadataClusterData;
import com.bilibili.cluster.scheduler.common.dto.caster.PodInfo;
import com.bilibili.cluster.scheduler.common.dto.clickhouse.clickhouse.params.CkTideExtFlowParams;
import com.bilibili.cluster.scheduler.common.entity.ExecutionFlowEntity;
import com.bilibili.cluster.scheduler.common.event.TaskEvent;

import javax.annotation.Resource;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @description:
 * @Date: 2024/12/5 11:54
 * @Author: nizhiqiang
 */
public abstract class AbstractCkTideWaitEventHandler extends AbstractTaskEventHandler {

    private static Integer CHECK_MINUTES = 30;

    @Resource
    BmrManagerService bmrManagerService;

    @Resource
    ComCasterService comCasterService;

    @Resource
    BmrMetadataService bmrMetadataService;

    @Resource
    ClickhouseService clickhouseService;

    @Override
    public boolean executeTaskEvent(TaskEvent taskEvent) throws Exception {
        Long flowId = taskEvent.getFlowId();
        ExecutionFlowEntity executionFlow = executionFlowService.getById(flowId);

        CkTideExtFlowParams ckTideExtFlowParams = executionFlowPropsService.getFlowExtParamsByCache(flowId, CkTideExtFlowParams.class);

        LocalDateTime start = LocalDateTime.now();
        Long clusterId = executionFlow.getClusterId();
        Long componentId = executionFlow.getComponentId();
        String clusterName = executionFlow.getClusterName();

        MetadataClusterData clusterData = bmrMetadataService.queryClusterDetail(clusterId);
        String appId = clusterData.getAppId();

        while (Duration.between(start, LocalDateTime.now()).toMinutes() < CHECK_MINUTES) {
            bmrManagerService.refreshPodInfo(clusterId, componentId, clusterName);

            Map<String, Object> labelMap = new HashMap<>();
            labelMap.put("app_id", appId);
            labelMap.put("scene", "clickhouse");
            List<PodInfo> podInfoList = comCasterService.queryPodList(Constants.CK_K8S_CLUSTER_ID, labelMap, null, null);
            List<PodInfo> runningPodList = new ArrayList<>();

            for (PodInfo podInfo : podInfoList) {
                String podStatus = podInfo.getPodStatus();
                if (!Constants.POD_STATUS_SUCCESS.equals(podStatus)) {
                    continue;
                }
                runningPodList.add(podInfo);
            }
            if (checkPodCount(taskEvent, ckTideExtFlowParams, runningPodList)) {
                logPersist(taskEvent, "潮汐成功");
                return true;
            }

            logPersist(taskEvent, "容器数量未达到，等待2分钟");
            Thread.sleep(Constants.ONE_MINUTES * 2);

        }


        logPersist(taskEvent, "超过30分钟容器数量未达标，检查超时");
        return false;
    }

    abstract protected boolean checkPodCount(TaskEvent taskEvent, CkTideExtFlowParams ckTideExtFlowParams, List<PodInfo> runningPodList);

}
