package com.bilibili.cluster.scheduler.api.service.scheduler.presto;

import cn.hutool.json.JSONUtil;
import com.bilibili.cluster.scheduler.api.service.bmr.resourceV2.BmrResourceV2ServiceImpl;
import com.bilibili.cluster.scheduler.api.service.caster.ComCasterServiceImpl;
import com.bilibili.cluster.scheduler.common.Constants;
import com.bilibili.cluster.scheduler.common.dto.caster.PodInfo;
import com.bilibili.cluster.scheduler.common.dto.caster.ResourceNodeInfo;
import com.bilibili.cluster.scheduler.common.dto.flow.req.DeployOneFlowReq;
import com.bilibili.cluster.scheduler.common.dto.bmr.resourceV2.TideNodeDetail;
import com.bilibili.cluster.scheduler.common.dto.presto.scaler.PrestoFastScalerExtFlowParams;
import com.bilibili.cluster.scheduler.common.dto.tide.flow.YarnTideExtFlowParams;
import com.bilibili.cluster.scheduler.common.enums.resourceV2.TideClusterType;
import com.bilibili.cluster.scheduler.common.enums.resourceV2.TideNodeStatus;
import com.bilibili.cluster.scheduler.common.dto.presto.tide.PrestoTideExtFlowParams;
import com.bilibili.cluster.scheduler.common.enums.flow.FlowDeployType;
import com.bilibili.cluster.scheduler.common.enums.flow.FlowEffectiveModeEnum;
import com.bilibili.cluster.scheduler.common.utils.LocalDateFormatterUtils;
import com.google.common.base.Preconditions;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.junit.Test;
import org.springframework.util.CollectionUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
public class PrestoTideTestUnit {

    final ComCasterServiceImpl comCasterService = new ComCasterServiceImpl();
    final BmrResourceV2ServiceImpl bmrResourceV2Service = new BmrResourceV2ServiceImpl();

    private int prestoClusterId = 126;
    private String appId = "datacenter.bmr-trino.trino2";

    @Test
    public void getPrestoTideOffFlowReq() {

        final DeployOneFlowReq deployOneFlowReq = new DeployOneFlowReq();
        deployOneFlowReq.setDeployType(FlowDeployType.PRESTO_TIDE_OFF);
        deployOneFlowReq.setUserName("bmr-presto-tide-user");
        deployOneFlowReq.setEffectiveMode(FlowEffectiveModeEnum.RESTART_EFFECTIVE);
        deployOneFlowReq.setParallelism(1);
        deployOneFlowReq.setTolerance(0);

        deployOneFlowReq.setClusterId(56L);
        deployOneFlowReq.setComponentId(84L);
        deployOneFlowReq.setRoleName("presto");
        deployOneFlowReq.setClusterName("prebsk-trino");
        deployOneFlowReq.setComponentName("Presto");

        final PrestoTideExtFlowParams flowParams = new PrestoTideExtFlowParams();
        flowParams.setYarnClusterId(2l);
        flowParams.setCurrentPod(4);
        flowParams.setRemainPod(3);
        flowParams.setAppId("datacenter.bmr-trino.trino2");
        flowParams.setTideOffEndTime(formatLocalDateTime(LocalDateTime.now().plusHours(1)));
        flowParams.setTideOffStartTime(formatLocalDateTime(LocalDateTime.now()));

        flowParams.setTideOnStartTime(formatLocalDateTime(LocalDateTime.now().plusHours(10)));
        flowParams.setTideOnStartTime(formatLocalDateTime(LocalDateTime.now().plusHours(11)));

        deployOneFlowReq.setExtParams(JSONUtil.toJsonStr(flowParams));
        deployOneFlowReq.setRemark("presto潮汐下线");
        System.out.println(JSONUtil.toJsonStr(deployOneFlowReq));
    }



    @Test
    public void getPrestoFastScalerFlowReq() {

        final DeployOneFlowReq deployOneFlowReq = new DeployOneFlowReq();
        deployOneFlowReq.setDeployType(FlowDeployType.PRESTO_FAST_EXPANSION);
        deployOneFlowReq.setUserName("bmr-presto-tide-user");
        deployOneFlowReq.setEffectiveMode(FlowEffectiveModeEnum.RESTART_EFFECTIVE);
        deployOneFlowReq.setParallelism(1);
        deployOneFlowReq.setTolerance(0);

        deployOneFlowReq.setClusterId(56L);
        deployOneFlowReq.setComponentId(84L);
        deployOneFlowReq.setRoleName("presto");
        deployOneFlowReq.setClusterName("prebsk-trino");
        deployOneFlowReq.setComponentName("Presto");

//        final YarnTideExtFlowParams flowParams = new YarnTideExtFlowParams();
//        flowParams.setYarnClusterId(2l);
//        flowParams.setAppId("datacenter.bmr-trino.trino2");
//        flowParams.setClusterType(TideClusterType.PRESTO);
//
//        flowParams.setExpectedCount(6);

        PrestoFastScalerExtFlowParams flowParams = new PrestoFastScalerExtFlowParams();
        flowParams.setHighPodNum(10);
        flowParams.setLowPodNum(6);

        deployOneFlowReq.setExtParams(JSONUtil.toJsonStr(flowParams));
        deployOneFlowReq.setRemark("presto快速缩容预发验证");
        System.out.println(JSONUtil.toJsonStr(deployOneFlowReq));
    }


    @Test
    public void getYarnTideFlowReq() {

        final DeployOneFlowReq deployOneFlowReq = new DeployOneFlowReq();
        deployOneFlowReq.setDeployType(FlowDeployType.YARN_TIDE_EXPANSION);
        deployOneFlowReq.setUserName("bmr-presto-tide-user");
        deployOneFlowReq.setEffectiveMode(FlowEffectiveModeEnum.RESTART_EFFECTIVE);
        deployOneFlowReq.setParallelism(1);
        deployOneFlowReq.setTolerance(0);

        deployOneFlowReq.setClusterId(2L);
        // deployOneFlowReq.setComponentId(84L);
        deployOneFlowReq.setRoleName("Yarn");
        deployOneFlowReq.setClusterName("常熟测试集群");
        // deployOneFlowReq.setComponentName("");

        final YarnTideExtFlowParams flowParams = new YarnTideExtFlowParams();
        flowParams.setYarnClusterId(2l);
        flowParams.setAppId("2");
        flowParams.setClusterType(TideClusterType.PRESTO);
        flowParams.setExpectedCount(3);

        deployOneFlowReq.setExtParams(JSONUtil.toJsonStr(flowParams));
        deployOneFlowReq.setRemark("yarn潮汐流程预发测试");
        System.out.println(JSONUtil.toJsonStr(deployOneFlowReq));
    }


    @Test
    public void getPrestoTideOnFlowReq() {

        final DeployOneFlowReq deployOneFlowReq = new DeployOneFlowReq();
        deployOneFlowReq.setDeployType(FlowDeployType.PRESTO_TIDE_ON);
        deployOneFlowReq.setUserName("bmr-presto-tide-user");
        deployOneFlowReq.setEffectiveMode(FlowEffectiveModeEnum.RESTART_EFFECTIVE);
        deployOneFlowReq.setParallelism(1);
        deployOneFlowReq.setTolerance(0);

        deployOneFlowReq.setClusterId(56L);
        deployOneFlowReq.setComponentId(84L);
        deployOneFlowReq.setRoleName("presto");
        deployOneFlowReq.setClusterName("prebsk-trino");
        deployOneFlowReq.setComponentName("Presto");

        final PrestoTideExtFlowParams flowParams = new PrestoTideExtFlowParams();
        flowParams.setYarnClusterId(2l);
        flowParams.setCurrentPod(4);
        flowParams.setRemainPod(3);
        flowParams.setAppId("datacenter.bmr-trino.trino2");
        flowParams.setTideOffEndTime(formatLocalDateTime(LocalDateTime.now().plusHours(1)));
        flowParams.setTideOffStartTime(formatLocalDateTime(LocalDateTime.now()));

        flowParams.setTideOnStartTime(formatLocalDateTime(LocalDateTime.now().plusHours(10)));
        flowParams.setTideOnStartTime(formatLocalDateTime(LocalDateTime.now().plusHours(11)));

        deployOneFlowReq.setExtParams(JSONUtil.toJsonStr(flowParams));
        deployOneFlowReq.setRemark("presto潮汐上线");
        System.out.println(JSONUtil.toJsonStr(deployOneFlowReq));
    }


    private String formatLocalDateTime(LocalDateTime time) {
        return LocalDateFormatterUtils.format(Constants.FMT_DATE_TIME, time);
    }

    @Test
    public void taintStatueCancel() {
        final ComCasterServiceImpl comCasterService = new ComCasterServiceImpl();
        comCasterService.updateNodeToTaintOff(prestoClusterId, "10.156.14.22", TideClusterType.PRESTO);
//        comCasterService.updateNodeToTaintOff(prestoClusterId, "10.157.148.31", TideClusterType.PRESTO);
//         comCasterService.updateNodeToTaintOff(prestoClusterId, "10.155.163.13");
//         comCasterService.updateNodeToTaintOff(prestoClusterId, "10.155.163.35");
//         comCasterService.updateNodeToTaintOff(prestoClusterId, "10.155.164.19");
//         comCasterService.updateNodeToTaintOff(prestoClusterId, "10.156.14.14");
//         comCasterService.updateNodeToTaintOff(prestoClusterId, "10.156.14.25");
//         comCasterService.updateNodeToTaintOff(prestoClusterId, "10.155.161.36");
//         comCasterService.updateNodeToTaintOff(prestoClusterId, "10.155.166.39");
//         comCasterService.updateNodeToTaintOff(prestoClusterId, "10.155.162.14");

    }

    @Test
    public void taintStatueTurnOn() {
        final ComCasterServiceImpl comCasterService = new ComCasterServiceImpl();
        boolean isTurnOn = comCasterService.updateNodeToTaintOn(prestoClusterId, "10.155.12.29", TideClusterType.PRESTO);
        System.out.println(isTurnOn);
        isTurnOn = comCasterService.updateNodeToTaintOn(prestoClusterId, "10.155.12.45", TideClusterType.PRESTO);
        System.out.println(isTurnOn);
    }


    @Test
    public void updateBmrTideNodeState() {
//        bmrResourceV2Service.updatePrestoNodeServiceAndStatus("jscs-olap-common-15", PrestoNodeStatus.AVAILABLE, "", "");
//        bmrResourceV2Service.updatePrestoNodeServiceAndStatus("jscs-olap-common-27", PrestoNodeStatus.AVAILABLE, "", "");
//        bmrResourceV2Service.updatePrestoNodeServiceAndStatus("jscs-olap-common-30", PrestoNodeStatus.AVAILABLE, "", "presto");
        bmrResourceV2Service.updateTideNodeServiceAndStatus("jscs-olap-trino-150", TideNodeStatus.AVAILABLE, "", "presto", TideClusterType.PRESTO);
        bmrResourceV2Service.updateTideNodeServiceAndStatus("jscs-olap-trino-146", TideNodeStatus.AVAILABLE, "", "presto", TideClusterType.PRESTO);
        bmrResourceV2Service.updateTideNodeServiceAndStatus("jscs-olap-trino-64", TideNodeStatus.AVAILABLE, "", "presto", TideClusterType.PRESTO);
        bmrResourceV2Service.updateTideNodeServiceAndStatus("jscs-olap-trino-147", TideNodeStatus.AVAILABLE, "", "presto", TideClusterType.PRESTO);
        bmrResourceV2Service.updateTideNodeServiceAndStatus("jscs-olap-trino-113", TideNodeStatus.AVAILABLE, "", "presto", TideClusterType.PRESTO);
        bmrResourceV2Service.updateTideNodeServiceAndStatus("jscs-olap-trino-100", TideNodeStatus.AVAILABLE, "", "presto", TideClusterType.PRESTO);
        bmrResourceV2Service.updateTideNodeServiceAndStatus("jscs-olap-trino-377", TideNodeStatus.AVAILABLE, "", "presto", TideClusterType.PRESTO);
        bmrResourceV2Service.updateTideNodeServiceAndStatus("jscs-olap-trino-364", TideNodeStatus.AVAILABLE, "", "presto", TideClusterType.PRESTO);
        bmrResourceV2Service.updateTideNodeServiceAndStatus("jscs-olap-trino-411", TideNodeStatus.AVAILABLE, "", "presto", TideClusterType.PRESTO);
    }

    @Test
    public void updateBmrTideNode() {
//        bmrResourceV2Service.updatePrestoNodeServiceAndStatus("jscs-olap-common-15", PrestoNodeStatus.STAIN, appId, "");
//        bmrResourceV2Service.updatePrestoNodeServiceAndStatus("jscs-olap-common-27", PrestoNodeStatus.STAIN, appId, "");
        bmrResourceV2Service.updateTideNodeServiceAndStatus("jscs-olap-common-30", TideNodeStatus.STAIN, appId, "", TideClusterType.PRESTO);
    }

    @Test
    public void testIsHoliday() {
        String currentDate = LocalDateFormatterUtils.formatDate(Constants.FMT_DAY, LocalDate.now().plus(3, ChronoUnit.DAYS));
        final boolean holiday = bmrResourceV2Service.isHoliday(currentDate);
        System.out.println(currentDate + " is holiday ? " + holiday);
    }


    @Test
    public void testTimeComparable() {
        String currentTime = LocalDateFormatterUtils.format(Constants.FMT_MINS, LocalDateTime.now());
        String cronTime1 = "20:10";
        String cronTime2 = "18:10";
        String cronTime3 = "19:10";
        final List<String> cronTimeList = Arrays.asList(cronTime1, cronTime2, cronTime3);
        currentTime = currentTime.split(" ")[1];
        System.out.println("currentTime is: " + currentTime);
        for (String cronTime : cronTimeList) {
            if (currentTime.compareTo(cronTime) < 0) {
                // do-nothing
            } else {
                System.out.println("should check cron time: " + cronTime);
            }
        }
    }

    @Test
    public void queryPrestoNodeInfo() {
        // 是否达成预期数量
        boolean isPrepareReady = false;

        // 查询当前已经处于污点中的节点列表
//        final Long flowId = taskEvent.getFlowId();
//        final PrestoTideExtFlowParams prestoTideExtFlowParams = flowPropsService.getFlowExtParamsByCache(flowId, PrestoTideExtFlowParams.class);

        final int remainPod = 1;
        final int currentPod = 3;

        int expectedShrinkPod = currentPod - remainPod;
        int alreadyAvailablePod = 0;

        List<TideNodeDetail> initAvailableNodeList = bmrResourceV2Service.queryTideOnBizUsedNodes(appId, TideNodeStatus.STAIN, TideClusterType.PRESTO);
        Map<String, TideNodeDetail> availableNodeMap = new HashMap<>();
        if (!CollectionUtils.isEmpty(initAvailableNodeList)) {
            log.info("initAvailableNodeList is {}", initAvailableNodeList);
        }

        if (!isPrepareReady) {
            // 获取资源池节点信息
            List<ResourceNodeInfo> nodeInfoList = comCasterService.queryAllNodeInfo(prestoClusterId);
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
                List<PodInfo> currentPodInfoList = comCasterService.queryPodListByNodeIp(prestoClusterId, nodeName);
                final TideNodeDetail tideNodeDetail = bmrResourceV2Service.queryTideNodeDetail(hostname);

                boolean available = isAvailableNode(currentPodInfoList, hostname, appId, tideNodeDetail);
                if (!available) {
                    continue;
                }
                log.info("presto tide offline available node is {} ", hostname);
                availableNodeMap.put(hostname, tideNodeDetail);


                final int coreNum = tideNodeDetail.getCoreNum();
                int equivalencePodNum = transferPodNum(coreNum);
                alreadyAvailablePod += equivalencePodNum;
                // 已达成预期节点数量
                if (alreadyAvailablePod >= expectedShrinkPod) {
                    log.info("already available pod ok");
                    isPrepareReady = true;
                    break;
                }
                if (availableNodeMap.size() > 0) {
                    break;
                }
            }
        }
        boolean isLatestTimeCheck = true;
        if (!isPrepareReady) {
            if (isLatestTimeCheck) {
                Double minShrinkPodValue = Math.floor(expectedShrinkPod * 0.85f);
                int minShrinkPod = minShrinkPodValue.intValue();
                if (minShrinkPod <= 0) {
                    minShrinkPod = 1;
                }
                log.info("minShrinkPod is {}.", minShrinkPod);
                if (alreadyAvailablePod >= minShrinkPod) {
                    isPrepareReady = true;
                } else {
                    log.info("at latestTimeCheck still not achieved minShrinkPod: {}.", minShrinkPod);
                }
            }
        }

        if (isPrepareReady) {
            log.info("isPrepareReady is true");
        } else {
            log.info("isPrepareReady is false");
        }
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
        boolean taintState = comCasterService.updateNodeToTaintOn(prestoClusterId, tideNodeDetail.getIp(), TideClusterType.PRESTO);
        if (!taintState) {
            return false;
        }
        // bmr更新节点占用状态（appId）
        return bmrResourceV2Service.updateTideNodeServiceAndStatus(hostname, TideNodeStatus.STAIN, appId, "", TideClusterType.PRESTO);
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
