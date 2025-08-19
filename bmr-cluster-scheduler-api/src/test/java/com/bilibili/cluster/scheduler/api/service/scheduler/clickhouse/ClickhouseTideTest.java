package com.bilibili.cluster.scheduler.api.service.scheduler.clickhouse;

import cn.hutool.json.JSONUtil;
import com.bilibili.cluster.scheduler.api.ApiApplicationServer;
import com.bilibili.cluster.scheduler.api.service.GlobalService;
import com.bilibili.cluster.scheduler.api.service.caster.ComCasterService;
import com.bilibili.cluster.scheduler.api.service.caster.ComCasterServiceImpl;
import com.bilibili.cluster.scheduler.api.service.clickhouse.clickhouse.ClickhouseService;
import com.bilibili.cluster.scheduler.common.Constants;
import com.bilibili.cluster.scheduler.common.dto.bmr.resourceV2.TideNodeDetail;
import com.bilibili.cluster.scheduler.common.dto.caster.PodInfo;
import com.bilibili.cluster.scheduler.common.dto.caster.ResourceNodeInfo;
import com.bilibili.cluster.scheduler.common.dto.clickhouse.clickhouse.ClickhouseDeployDTO;
import com.bilibili.cluster.scheduler.common.dto.clickhouse.clickhouse.templates.template.TemplatePodTemplate;
import com.bilibili.cluster.scheduler.common.dto.tide.resource.CkTideResource;
import com.bilibili.cluster.scheduler.common.enums.resourceV2.TideClusterType;
import com.bilibili.cluster.scheduler.common.enums.resourceV2.TideNodeStatus;
import com.google.common.base.Preconditions;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.apache.http.util.Asserts;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @description:
 * @Date: 2025/3/24 17:36
 * @Author: nizhiqiang
 */

@RunWith(SpringRunner.class)
@SpringBootTest(classes = ApiApplicationServer.class)
@Slf4j
public class ClickhouseTideTest {


    private static final String appId = "infra.caster.ct-replica";

    private static final String hostName = "10.155.15.25";

    private static Pattern indexPattern = Pattern.compile("(\\d{2})-0$");

    private final static Double FINE_RESOURCE_REQUIRE_PERCENT = 0.92d;

    private final static Double MIN_RESOURCE_REQUIRE_PERCENT = 0.7d;

    private final static Double LEAST_RESOURCE_REQUIRE_PERCENT = 0.4d;

    @Resource
    ComCasterService comCasterService;

    @Resource
    ClickhouseService clickhouseService;

    @Resource
    GlobalService globalService;

    @Test
    public void taintStatueTurnOn() {
//        final ComCasterServiceImpl comCasterService = new ComCasterServiceImpl();
//        boolean isTurnOn = comCasterService.updateNodeToTaintOn(Constants.CK_K8S_CLUSTER_ID, hostName, TideClusterType.CLICKHOUSE);
//        System.out.println(isTurnOn);


        final ComCasterServiceImpl comCasterService = new ComCasterServiceImpl();
        boolean isTurnOn = comCasterService.updateNodeToTaintOff(Constants.CK_K8S_CLUSTER_ID, hostName, TideClusterType.CLICKHOUSE);
        System.out.println(isTurnOn);
    }

    @Test
    public void testPodList() {
        List<PodInfo> needRemovePodList = comCasterService.queryPodListByNodeIp(Constants.CK_K8S_CLUSTER_ID, Constants.CK_K8S_TEST_HOST_IP_1);

        System.out.println("needRemovePodList = " + JSONUtil.toJsonStr(needRemovePodList));
        Set<Integer> needRemovePodIndexSet = needRemovePodList.stream()
                .map(PodInfo::getName)
                .map(this::getPodIndex)
                .filter(index -> index > 0)
                .collect(Collectors.toSet());

        System.out.println("needRemovePodIndexSet = " + JSONUtil.toJsonStr(needRemovePodIndexSet));

    }

    private Integer getPodIndex(String podName) {
        Matcher matcher = indexPattern.matcher(podName);
        if (matcher.find()) {
            int index = Integer.parseInt(matcher.group(1));
            return index;
        }

        return -1;
    }

    @Test
    public void testCkTideOffWait() {
        checkAndPrepareNodes(false);
    }


    private TemplatePodTemplate getCkStableTemplate(long configId) {
        ClickhouseDeployDTO clickhouseDeployDTO = clickhouseService.buildClickhouseDeployDTO(configId);
        List<TemplatePodTemplate> podTemplateList = clickhouseDeployDTO.getChTemplate().getPodTemplates();
        TemplatePodTemplate ckStableTemplate = podTemplateList.stream()
                .filter(podTemplate -> podTemplate.getName().equals(Constants.CK_STABLE_TEMPLATE))
                .findFirst().orElse(null);
        Asserts.notNull(ckStableTemplate, "clickhouse stable pod template is null");
        return ckStableTemplate;
    }

    private boolean checkAndPrepareNodes(boolean isLatestTimeCheck) {
        // 是否达成预期数量

        // 查询当前已经处于污点中的节点列表
        String appId = "infra.caster.ct-replica";

        final int remainPod = 1;
        final int currentPod = 3;

//        预期缩容的pod数
        int expectedShrinkPod = currentPod - remainPod;

        TemplatePodTemplate ckStableTemplate = getCkStableTemplate(2630);
//        容器所需最小的cpu和内存
        Integer cpuReq = ckStableTemplate.getResources().getCpuReq();
        Integer memReq = ckStableTemplate.getResources().getMemReq();

        CkTideResource ckTideResource = new CkTideResource(expectedShrinkPod, FINE_RESOURCE_REQUIRE_PERCENT, MIN_RESOURCE_REQUIRE_PERCENT, LEAST_RESOURCE_REQUIRE_PERCENT, cpuReq, memReq);

        Map<String, TideNodeDetail> availableNodeMap = new HashMap<>();
        boolean isPrepareReady = checkAvaliableNode(ckTideResource, availableNodeMap, appId);

        if (!isPrepareReady) {
            isPrepareReady = checkComResource(appId, ckTideResource, availableNodeMap);
        }
        if (!isPrepareReady) {
            if (isLatestTimeCheck) {
                isPrepareReady = lastTimeCheck(ckTideResource);
            }
        }

        if (isPrepareReady) {
            log.info(String.format("available taint node prepare ready. already memory is %s, already Available cpu is %s"
                    , ckTideResource.getCurrentMemory(), ckTideResource.getCurrentCpu()));

            Collection<TideNodeDetail> nodeList = availableNodeMap.values();
            log.info("node list is {}", JSONUtil.toJsonStr(nodeList));
        }
        return false;
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
    private boolean checkAvaliableNode(CkTideResource ckTideResource, Map<String, TideNodeDetail> availableNodeMap, String appId) {
        boolean isPrepareReady = false;
        List<TideNodeDetail> initAvailableNodeList = globalService.getBmrResourceV2Service().queryTideOnBizUsedNodes(appId, TideNodeStatus.STAIN, TideClusterType.CLICKHOUSE);
        if (!CollectionUtils.isEmpty(initAvailableNodeList)) {
            for (TideNodeDetail nodeDetail : initAvailableNodeList) {
//                节点设为污点
                boolean isSuc = comCasterService.updateNodeToTaintOn(Constants.CK_K8S_CLUSTER_ID, nodeDetail.getIp(), TideClusterType.CLICKHOUSE);
                if (!isSuc) {
                    log.info("设置节点不可调度失败,节点名称: " + nodeDetail.getHostName());
                    continue;
                }
                log.info("will use already taint node on yarn expansion: " + nodeDetail.getHostName());
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

    /**
     * 检查并准备COM平台的资源节点，确保满足任务事件的资源需求。
     *
     * @param taskEvent        任务事件对象，包含任务的相关信息。
     * @param appId            应用程序的唯一标识符，用于过滤资源节点。
     * @param ckTideResource   资源对象，用于记录和管理CPU和内存资源。
     * @param availableNodeMap 可用节点映射，用于存储符合条件的节点信息。
     * @return 返回一个布尔值，表示是否成功准备足够的资源节点。
     */
    private boolean checkComResource(String appId, CkTideResource ckTideResource, Map<String, TideNodeDetail> availableNodeMap) {
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
            log.info("更新com平台空闲资源节点,开启污点调度,节点名称: " + hostname);
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

    /**
     * 最后一次检查，如果已经满足预期，则直接返回true
     * 最后一次检查的指标会比之前更低
     *
     * @param taskEvent
     * @param ckTideResource
     * @return
     */
    private boolean lastTimeCheck(CkTideResource ckTideResource) {
        boolean isPrepareReady = false;
        double minCpu = ckTideResource.getMinCpu();
        int minMemory = ckTideResource.getMinMemory();
        String minMessage = String.format("min memory is %s, min cpu is %s.", minMemory, minCpu);
        log.info(minMessage);
        if (ckTideResource.isMin()) {
            isPrepareReady = true;
        } else {
            double leastCpu = ckTideResource.getLeastCpu();
            int leastMemory = ckTideResource.getLeastMemory();
            String leastMessage = String.format("least memory is %s, min cpu is %s.", leastMemory, leastCpu);

            log.info(leastMessage);
            if (ckTideResource.isLeast()) {
                isPrepareReady = true;
            }
        }
        return isPrepareReady;
    }
}
