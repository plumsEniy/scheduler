package com.bilibili.cluster.scheduler.api.event.tide;


import cn.hutool.core.thread.ThreadUtil;
import com.bilibili.cluster.scheduler.api.bean.SpringApplicationContext;
import com.bilibili.cluster.scheduler.api.event.dolphinScheduler.AbstractDolphinSchedulerEventHandler;
import com.bilibili.cluster.scheduler.api.service.GlobalService;
import com.bilibili.cluster.scheduler.api.service.flow.ExecutionFlowPropsService;
import com.bilibili.cluster.scheduler.api.service.flow.ExecutionFlowService;
import com.bilibili.cluster.scheduler.common.Constants;
import com.bilibili.cluster.scheduler.common.dto.bmr.config.model.FileDownloadData;
import com.bilibili.cluster.scheduler.common.dto.bmr.metadata.MetadataClusterData;
import com.bilibili.cluster.scheduler.common.dto.bmr.resource.model.ResourceHostInfo;
import com.bilibili.cluster.scheduler.common.dto.tide.flow.TideExtFlowParams;
import com.bilibili.cluster.scheduler.common.dto.yarn.RMInfoObj;
import com.bilibili.cluster.scheduler.common.entity.ExecutionFlowEntity;
import com.bilibili.cluster.scheduler.common.entity.ExecutionNodeEntity;
import com.bilibili.cluster.scheduler.common.enums.bmr.config.FileOperateType;
import com.bilibili.cluster.scheduler.common.enums.flow.FlowReleaseScopeType;
import com.bilibili.cluster.scheduler.common.enums.scheduler.DolpTaskType;
import com.bilibili.cluster.scheduler.common.event.TaskEvent;
import com.google.common.base.Preconditions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.*;

@Slf4j
@Component
public abstract class AbstractYarnNodeShrinkEventHandler extends AbstractDolphinSchedulerEventHandler {

    @Resource
    GlobalService globalService;

    @Resource
    ExecutionFlowService flowService;

    @Resource
    ExecutionFlowPropsService flowPropsService;

    /**
     * 下线yarn节点,仅在阶段1执行
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

    protected TideExtFlowParams getTideFlowExtParams(long flowId){
        return flowPropsService.getFlowExtParamsByCache(flowId, TideExtFlowParams.class);
    }


    @Override
    protected Map<String, Object> getDolphinExecuteEnv(TaskEvent taskEvent, List<String> hostList) {

        Map<String, Object> instanceEnv = new LinkedHashMap<>();
        final Long flowId = taskEvent.getFlowId();
        final ExecutionFlowEntity flowEntity = flowService.getById(flowId);
        final TideExtFlowParams tideExtFlowParams = getTideFlowExtParams(flowId);
        final long yarnClusterId = tideExtFlowParams.getYarnClusterId();

        final MetadataClusterData metadataClusterData = globalService.getBmrMetadataService().queryClusterDetail(yarnClusterId);
        Preconditions.checkNotNull(metadataClusterData, "yarn cluster info is null");
        instanceEnv.put(Constants.COMPONENT_ROLE, metadataClusterData.getUpperService());
        instanceEnv.put(Constants.COMPONENT_CLUSTER, metadataClusterData.getClusterName());
        instanceEnv.put(Constants.FLOW_ID, flowEntity.getId());
        instanceEnv.put(Constants._JOB_EXCUTE_TYPE, DolpTaskType.JOB_AGENT.name());
        log.info("JOB_EXECUTE_TYPE :" + flowEntity.getJobExecuteType());
        instanceEnv.put(Constants.RELASE_SCOPE, FlowReleaseScopeType.GRAY_RELEASE.name());
        instanceEnv.put(Constants.BATCH_ID, taskEvent.getBatchId());

        // 潮汐退避脚本额外参数
        instanceEnv.put(Constants.NODEMANAGER_IS_EXECUTE, Constants.TRUE);
        instanceEnv.put(Constants.SPARK_IS_EXECUTE, Constants.TRUE);
        instanceEnv.put(Constants.AMIYA_IS_EXECUTE, Constants.TRUE);

        StringJoiner joiner = new StringJoiner(Constants.COMMA);
        hostList.forEach(joiner::add);
        // 机器列表
        instanceEnv.put(Constants.SYSTEM_JOBAGENT_EXEC_HOSTS, joiner.toString());

        // 更新yarn-include文件
        RMInfoObj rmInfo = globalService.getBmrResourceService().queryRMComponentIdByClusterId(yarnClusterId);
        StringJoiner rmHostInfoJoiner = new StringJoiner(Constants.COMMA);
        List<String> rmHostList = rmInfo.getRmHostList();
        rmHostList.forEach(rmHostInfoJoiner::add);
        String rmHostValue = rmHostInfoJoiner.toString();
        instanceEnv.put(Constants.SUB_SYSTEM_HOST_LIST, rmHostValue);

        String msg = String.format("潮汐发布新增额外环境变量: key=%s, value=%s",
                Constants.SUB_SYSTEM_HOST_LIST, rmHostValue);
        logPersist(taskEvent, msg);

        List<ResourceHostInfo> resourceHostInfoList = globalService.getBmrResourceService().queryHostListByName(hostList);
        List<String> ipList = new ArrayList<>();
        resourceHostInfoList.stream().forEach(node -> ipList.add(node.getIp()));
        globalService.getBmrConfigService().updateFileIpList(rmInfo.getComponentId(),
                Constants.YARN_INCLUDE, FileOperateType.REMOVE, ipList);
        logPersist(taskEvent, "更新yarn-include成功");

        ThreadUtil.sleep(Constants.ONE_SECOND * 2);

        FileDownloadData fileDownloadData = globalService.getBmrConfigService().queryDownloadInfoByComponentId(
                rmInfo.getComponentId(), Constants.YARN_INCLUDE);
        instanceEnv.put(Constants.YARN_INCLUDE_DOWNLOAD_URL, fileDownloadData.getDownloadUrl());
        instanceEnv.put(Constants.YARN_INCLUDE_FILE_MD5, fileDownloadData.getFileMd5());
        instanceEnv.put(Constants.FLOW_ENV_KEY, SpringApplicationContext.getEnv());
        instanceEnv.put(Constants.YARN_RM_COMPONENT_ID_KEY, rmInfo.getComponentId());

        String yarnIncludeMsg = String.format("新增ResourceManager白名单下载链接: %s\n md5=%s",
                fileDownloadData.getDownloadUrl(), fileDownloadData.getFileMd5());
        logPersist(taskEvent, yarnIncludeMsg);

        return instanceEnv;
    }
}
