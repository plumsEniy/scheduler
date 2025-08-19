package com.bilibili.cluster.scheduler.api.event.hbo.jobParams;

import cn.hutool.json.JSONUtil;
import com.bilibili.cluster.scheduler.api.event.AbstractTaskEventHandler;
import com.bilibili.cluster.scheduler.api.service.flow.ExecutionFlowPropsService;
import com.bilibili.cluster.scheduler.api.service.flow.ExecutionNodePropsService;
import com.bilibili.cluster.scheduler.api.service.flow.ExecutionNodeService;
import com.bilibili.cluster.scheduler.api.service.hbo.HboService;
import com.bilibili.cluster.scheduler.common.dto.flow.prop.BaseFlowExtPropDTO;
import com.bilibili.cluster.scheduler.common.dto.hbo.model.HboJob;
import com.bilibili.cluster.scheduler.common.dto.hbo.model.HboJobInfo;
import com.bilibili.cluster.scheduler.common.dto.hbo.pararms.HboJobParamsUpdateFlowExtParams;
import com.bilibili.cluster.scheduler.common.dto.hbo.pararms.HboJobParamsUpdateJobExtParams;
import com.bilibili.cluster.scheduler.common.entity.ExecutionNodeEntity;
import com.bilibili.cluster.scheduler.common.enums.NodeExecuteStatusEnum;
import com.bilibili.cluster.scheduler.common.enums.event.EventTypeEnum;
import com.bilibili.cluster.scheduler.common.event.TaskEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.*;

/**
 * @description:
 * @Date: 2024/12/30 16:27
 * @Author: nizhiqiang
 */
@Slf4j
@Component
public class HboJobParamsUpdateEventHandler extends AbstractTaskEventHandler {

    @Resource
    ExecutionNodePropsService executionNodePropsService;

    @Resource
    ExecutionFlowPropsService executionFlowPropsService;

    @Resource
    ExecutionNodeService executionNodeService;

    @Resource
    HboService hboService;

    @Override
    public boolean executeTaskEvent(TaskEvent taskEvent) throws Exception {
        logPersist(taskEvent, "hbo job params update");
        Long nodeId = taskEvent.getExecutionNode().getId();
        String jobId = taskEvent.getExecutionNode().getNodeName();
        Long flowId = taskEvent.getFlowId();
        ExecutionNodeEntity executionNode = executionNodeService.getById(nodeId);
        NodeExecuteStatusEnum nodeStatus = executionNode.getNodeStatus();
        HboJobParamsUpdateJobExtParams jobExtParams = initAndGetHboParams(taskEvent);

        BaseFlowExtPropDTO baseFlowExtPropDTO = executionFlowPropsService.getFlowPropByFlowId(flowId, BaseFlowExtPropDTO.class);
        String flowExtParamStr = baseFlowExtPropDTO.getFlowExtParams();
        HboJobParamsUpdateFlowExtParams flowExtParam = JSONUtil.toBean(flowExtParamStr, HboJobParamsUpdateFlowExtParams.class);

//        回滚状态
        if (nodeStatus.isInRollback()) {
            String beforeParams = jobExtParams.getBeforeParams();
            if (StringUtils.isEmpty(beforeParams)) {
                logPersist(taskEvent, "回滚job, 变更前参数为空，将删除该任务");
                hboService.deleteJob(Arrays.asList(jobId));
            } else {
                logPersist(taskEvent, String.format("回滚job, 变更参数为%s", beforeParams));
                upsertJob(jobId, beforeParams);
            }
            logPersist(taskEvent, "回滚完成");
            return true;
        }

        List<String> jobIdList = new LinkedList<>();
        jobIdList.add(jobId);
        List<HboJobInfo> hboJobInfoList = hboService.queryJobListByJobId(jobIdList);

        Map<String, String> addParamsMap = flowExtParam.getAddParamsMap();
        Map<String, String> removeParamsMap = flowExtParam.getRemoveParamsMap();

        if (CollectionUtils.isEmpty(hboJobInfoList)) {
            String paramList = JSONUtil.toJsonStr(addParamsMap);
            logPersist(taskEvent, "当前任务不存在将会创建任务, param list is " + paramList);
            upsertJob(jobId, paramList);
        } else {
            logPersist(taskEvent, "当前任务存在将更新参数");
            logPersist(taskEvent, "新增参数列表为 " + JSONUtil.toJsonStr(addParamsMap));
            logPersist(taskEvent, "删除参数列表为 " + JSONUtil.toJsonStr(removeParamsMap));
            hboService.updateJobParams(Arrays.asList(jobId), addParamsMap, removeParamsMap);
        }

        hboJobInfoList = hboService.queryJobListByJobId(Arrays.asList(jobId));
        if (CollectionUtils.isEmpty(hboJobInfoList)) {
            throw new RuntimeException("job添加或者修改后无法查询到");
        }
        HboJobInfo hboJobInfo = hboJobInfoList.get(0);
        jobExtParams.setAfterParams(hboJobInfo.getParamList());
        logPersist(taskEvent, "变更后的参数为:" + hboJobInfo.getParamList());
        executionNodePropsService.saveNodeProp(nodeId, jobExtParams);
        return true;
    }

    private void upsertJob(String jobId, String beforeParams) {
        HboJob hboJob = new HboJob();
        hboJob.setJobId(jobId);
        hboJob.setParamList(beforeParams);
        List<HboJob> hboJobList = new LinkedList<>();
        hboJobList.add(hboJob);
        hboService.upsertJob(hboJobList);
    }

    /**
     * 初始化hbo变更前参数
     *
     * @param taskEvent
     */
    private HboJobParamsUpdateJobExtParams initAndGetHboParams(TaskEvent taskEvent) {
        Long nodeId = taskEvent.getExecutionNode().getId();
        String nodeName = taskEvent.getExecutionNode().getNodeName();
        HboJobParamsUpdateJobExtParams hboJobParamsUpdateJobExtParams = executionNodePropsService.queryNodePropsByNodeId(nodeId, HboJobParamsUpdateJobExtParams.class);

        if (!Objects.isNull(hboJobParamsUpdateJobExtParams)) {
            return hboJobParamsUpdateJobExtParams;
        }

        hboJobParamsUpdateJobExtParams = new HboJobParamsUpdateJobExtParams();
        hboJobParamsUpdateJobExtParams.setNodeId(nodeId);
        List<HboJobInfo> hboJobInfoList = hboService.queryJobListByJobId(Arrays.asList(nodeName));
        if (CollectionUtils.isEmpty(hboJobInfoList)) {
            logPersist(taskEvent, "变更前job不存在");
        } else {
            HboJobInfo hboJobInfo = hboJobInfoList.get(0);
            String paramList = hboJobInfo.getParamList();
            hboJobParamsUpdateJobExtParams.setBeforeParams(paramList);
            logPersist(taskEvent, "变更前job参数为" + paramList);
        }
        executionNodePropsService.saveNodeProp(nodeId, hboJobParamsUpdateJobExtParams);
        return hboJobParamsUpdateJobExtParams;
    }

    @Override
    public EventTypeEnum getHandleEventType() {
        return EventTypeEnum.HBO_JOB_PARAMS_UPDATE;
    }
}
