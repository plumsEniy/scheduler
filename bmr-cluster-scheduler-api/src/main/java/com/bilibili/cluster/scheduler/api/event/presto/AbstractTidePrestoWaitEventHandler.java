package com.bilibili.cluster.scheduler.api.event.presto;

import com.bilibili.cluster.scheduler.api.event.AbstractTaskEventHandler;
import com.bilibili.cluster.scheduler.api.service.bmr.manager.BmrManagerService;
import com.bilibili.cluster.scheduler.api.service.bmr.metadata.BmrMetadataService;
import com.bilibili.cluster.scheduler.api.service.caster.ComCasterService;
import com.bilibili.cluster.scheduler.api.service.presto.PrestoService;
import com.bilibili.cluster.scheduler.common.Constants;
import com.bilibili.cluster.scheduler.common.dto.bmr.metadata.MetadataClusterData;
import com.bilibili.cluster.scheduler.common.dto.caster.PodInfo;
import com.bilibili.cluster.scheduler.common.dto.presto.PrestoYamlObj;
import com.bilibili.cluster.scheduler.common.entity.ExecutionFlowEntity;
import com.bilibili.cluster.scheduler.common.enums.presto.PodType;
import com.bilibili.cluster.scheduler.common.event.TaskEvent;

import javax.annotation.Resource;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * @description:
 * @Date: 2024/12/5 11:54
 * @Author: nizhiqiang
 */
public abstract class AbstractTidePrestoWaitEventHandler extends AbstractTaskEventHandler {

    private static Integer CHECK_MINUTES = 30;

    @Resource
    protected BmrManagerService bmrManagerService;

    @Resource
    protected ComCasterService comCasterService;

    @Resource
    protected BmrMetadataService bmrMetadataService;

    @Resource
    protected PrestoService prestoService;

    @Override
    public boolean executeTaskEvent(TaskEvent taskEvent) throws Exception {
        Long flowId = taskEvent.getFlowId();
        ExecutionFlowEntity executionFlow = executionFlowService.getById(flowId);
        LocalDateTime start = LocalDateTime.now();
        Long clusterId = getClusterId(taskEvent);
        Long componentId = getComponentId(taskEvent);
        MetadataClusterData clusterData = bmrMetadataService.queryClusterDetail(clusterId);
        String clusterName = clusterData.getClusterName();
        logPersist(taskEvent,String.format("发布集群名称为%s", clusterName));


        String appId = clusterData.getAppId();
        String networkEnvironment = clusterData.getNetworkEnvironment().getEnv();

        int checkTime = 0;

        while (Duration.between(start, LocalDateTime.now()).toMinutes() < CHECK_MINUTES) {
            bmrManagerService.refreshPodInfo(clusterId, componentId, clusterName);

            String podSelector = String.format("app_id=%s,env=%s", appId, networkEnvironment);
            List<PodInfo> podInfoList = comCasterService.queryPodList(prestoService.getPrestoCasterClusterId(), podSelector, null, null);
            List<PodInfo> workPodList = new ArrayList<>();

            for (PodInfo podInfo : podInfoList) {
                PodType podType = PodType.getPodType(podInfo.getName());
                if (!podType.equals(PodType.WORKER)) {
                    continue;
                }

                // 对所有pod做检查，即使pod缩容卡住，暂不影响其他相关流程
                if (checkAllPodState(executionFlow)) {
                    workPodList.add(podInfo);
                    continue;
                }

                String podStatus = podInfo.getPodStatus();
                if (!Constants.POD_STATUS_SUCCESS.equals(podStatus)) {
                    continue;
                }
                workPodList.add(podInfo);
            }
            if (checkPodCount(taskEvent, workPodList)) {
                logPersist(taskEvent, "潮汐成功");
                PrestoYamlObj prestoYamlObj = prestoService.buildPrestoYamlObj(clusterData.getRunningConfigId(), "test");
                String startTemplate = prestoYamlObj.getStart();
                if (!startTemplate.contains(Constants.SLEEP_SH)) {
                    logPersist(taskEvent, "启动脚本不为sleep.sh，执行active");
                    prestoService.activeCluster(clusterName, networkEnvironment);
                }

                return true;
            }

            releaseTideResourceForExpansion(++checkTime, taskEvent, workPodList);

            logPersist(taskEvent, "容器数量未达到，等待2分钟");
            Thread.sleep(Constants.ONE_MINUTES * 2);
        }


        logPersist(taskEvent, "超过30分钟容器数量未达标，检查超时");
        return false;
    }

    protected void releaseTideResourceForExpansion(int checkTime, TaskEvent taskEvent, List<PodInfo> readyPodList) {
       // do-nothing
    }

    abstract protected boolean checkPodCount(TaskEvent taskEvent, List<PodInfo> workPodList);


    protected Long getComponentId(TaskEvent taskEvent) {
        Long flowId = taskEvent.getFlowId();
        ExecutionFlowEntity executionFlow = executionFlowService.getById(flowId);
        return executionFlow.getComponentId();
    }

    protected Long getClusterId(TaskEvent taskEvent) {
        Long flowId = taskEvent.getFlowId();
        ExecutionFlowEntity executionFlow = executionFlowService.getById(flowId);
        return executionFlow.getClusterId();
    }

    protected boolean checkAllPodState(ExecutionFlowEntity executionFlow) {
        return false;
    }

}
