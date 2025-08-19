package com.bilibili.cluster.scheduler.api.event.tide;


import cn.hutool.core.thread.ThreadUtil;
import com.bilibili.cluster.scheduler.api.event.BatchedTaskEventHandler;
import com.bilibili.cluster.scheduler.api.service.GlobalService;
import com.bilibili.cluster.scheduler.api.service.bmr.yarn.YarnNodeManagerService;
import com.bilibili.cluster.scheduler.api.service.flow.ExecutionFlowPropsService;
import com.bilibili.cluster.scheduler.api.service.flow.ExecutionFlowService;
import com.bilibili.cluster.scheduler.common.Constants;
import com.bilibili.cluster.scheduler.common.dto.bmr.metadata.MetadataComponentData;
import com.bilibili.cluster.scheduler.common.dto.bmr.resource.ComponentNodeDetail;
import com.bilibili.cluster.scheduler.common.dto.tide.flow.TideExtFlowParams;
import com.bilibili.cluster.scheduler.common.entity.ExecutionNodeEntity;
import com.bilibili.cluster.scheduler.common.enums.bmr.config.FileOperateType;
import com.bilibili.cluster.scheduler.common.event.TaskEvent;
import com.google.common.base.Preconditions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Component
public abstract class AbstractTideYarnNodeGracefulOfflineEventHandler extends BatchedTaskEventHandler {

    @Resource
    ExecutionFlowService flowService;

    @Resource
    ExecutionFlowPropsService flowPropsService;

    @Resource
    GlobalService globalService;

    @Resource
    YarnNodeManagerService yarnNodeManagerService;

    private static final int stopWaitTimeSecond = 60 * 10;
    private static final int stopWaitLogUploadTimeSecond = 600;

    @Value("${tide.graceful.off.wait.minutes:21}")
    int tideGracefulOffWaitMinutes;

    protected TideExtFlowParams getTideFlowExtParams(long flowId){
        return flowPropsService.getFlowExtParamsByCache(flowId, TideExtFlowParams.class);
    }

    @Override
    public boolean batchExecEvent(TaskEvent taskEvent, List<ExecutionNodeEntity> nodeEntityList) throws Exception {
        logPersist(taskEvent, "yarn节点下线任务优雅退出最大等待时间为：" + tideGracefulOffWaitMinutes + "分钟");

        final List<String> offHostList = nodeEntityList.stream().map(ExecutionNodeEntity::getNodeName).collect(Collectors.toList());
        yarnNodeManagerService.amiyaGracefullyOff(offHostList, stopWaitTimeSecond, stopWaitLogUploadTimeSecond);
        String message = "amiya强制下线接口调用成功.";
        logPersist(taskEvent, message);

        final Long flowId = taskEvent.getFlowId();
        TideExtFlowParams tideExtFlowParams = getTideFlowExtParams(flowId);
        final long yarnClusterId = tideExtFlowParams.getYarnClusterId();
        final List<MetadataComponentData> componentDataList = globalService.getBmrMetadataService().queryComponentListByClusterId(yarnClusterId);
        MetadataComponentData nodeManagerComponentData = null;
        MetadataComponentData resourceManagerComponentData = null;
        MetadataComponentData sparkEssWorkComponentData = null;
        MetadataComponentData sparkEssMasterComponentData = null;
        MetadataComponentData amiyaComponentData = null;

        for (MetadataComponentData metadataComponentData : componentDataList) {
            final String componentName = metadataComponentData.getComponentName();
            switch (componentName) {
                case "ResourceManager":
                    resourceManagerComponentData = metadataComponentData;
                    break;
                case "NodeManager":
                    nodeManagerComponentData = metadataComponentData;
                    break;
                case "Amiya":
                    amiyaComponentData = metadataComponentData;
                    break;
                case "SparkEssMaster":
                    sparkEssMasterComponentData = metadataComponentData;
                    break;
                case "SparkEssWorker":
                    sparkEssWorkComponentData = metadataComponentData;
                    break;
                default:
                    log.info("find other component name is {}", componentName);
            }
        }
        Preconditions.checkNotNull(nodeManagerComponentData, "NodeManager metadata not exist");
        Preconditions.checkNotNull(resourceManagerComponentData, "ResourceManager metadata not exist");
        Preconditions.checkNotNull(sparkEssWorkComponentData, "SparkEssWorker metadata not exist");
        Preconditions.checkNotNull(sparkEssMasterComponentData, "SparkEssMaster metadata not exist");
        Preconditions.checkNotNull(amiyaComponentData, "Amiya metadata not exist");

        // 操作sparkEssMaster
        final int sparkEssMasterComponentId = sparkEssMasterComponentData.getId();
        ComponentNodeDetail sparkEssMasterRunningNode = null;
        List<ComponentNodeDetail> sparkEssMasterNodeDetailList = globalService.getBmrResourceService().queryComponentNodeList(yarnClusterId, sparkEssMasterComponentId);
        // 当前仅有一台spark-ess-master
        if (CollectionUtils.isEmpty(sparkEssMasterNodeDetailList)) {
            logPersist(taskEvent, "sparkEssMaster node list is blank");
        } else {
            List<ComponentNodeDetail> componentNodeRunningStateHosts = sparkEssMasterNodeDetailList.stream()
                    .filter(x -> Constants.APPLICATION_STATE_RUNNING.equals(x.getApplicationState()))
                    .collect(Collectors.toList());
            if (CollectionUtils.isEmpty(componentNodeRunningStateHosts)) {
                logPersist(taskEvent, "sparkEssMaster running node is blank");
            } else {
                sparkEssMasterRunningNode = componentNodeRunningStateHosts.get(0);
            }
        }
        // 更新sparkEssMaster黑名单，把主机信息加入黑名单列表
        List<String> addHostSuffixList = new ArrayList<>();
        offHostList.forEach(host -> {
            if (!host.contains(Constants.HOST_SUFFIX)) {
                addHostSuffixList.add(host.concat(Constants.HOST_SUFFIX));
            } else {
                addHostSuffixList.add(host);
            }
        });
        Boolean updateConfigBlackListSuccess = globalService.getBmrConfigService().updateFileIpList(
                sparkEssMasterComponentId, Constants.SPARK_BLACK_LIST, FileOperateType.ADD, addHostSuffixList);
        if (updateConfigBlackListSuccess) {
            logPersist(taskEvent, "sparkEssMaster添加黑名单配置成功.");
        }
        // refresh spark ess master black list
        if (!Objects.isNull(sparkEssMasterRunningNode)) {
            boolean sparkEssMasterRefreshStatus = globalService.getSparkEssMasterService().addBlackList(addHostSuffixList, sparkEssMasterRunningNode.getHostName());
            if (sparkEssMasterRefreshStatus) {
                logPersist(taskEvent, "sparkEssMaster刷新黑名单列表成功.");
            }
        } else {
            logPersist(taskEvent, "sparkEssMaster运行中节点不存在");
        }

        final LocalDateTime startTime = taskEvent.getEventEntity().getStartTime();
        while (true) {
            Duration duration = Duration.between(startTime, LocalDateTime.now()).abs();
            // if (duration.toMinutes() >= 21) {
            if (duration.toMinutes() >= tideGracefulOffWaitMinutes) {
                message = "时间超过21min，将所有节点强制下线.";
                logPersist(taskEvent, message);
                break;
            }
            ThreadUtil.safeSleep(Constants.ONE_MINUTES);
        }
        return true;
    }

    @Override
    public int getMinLoopWait() {
        return 30_000;
    }

    @Override
    public int getMaxLoopStep() {
        return 100_000;
    }

    @Override
    public void printLog(TaskEvent taskEvent, String logContent) {
        log.info(logContent);
    }

    @Override
    public int logMod() {
        return 10;
    }

    /**
     * 等待任务优雅退出（yarn缩容前置操作）,仅在阶段1执行
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
