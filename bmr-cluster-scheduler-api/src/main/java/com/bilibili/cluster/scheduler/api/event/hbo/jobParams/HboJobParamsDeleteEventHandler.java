package com.bilibili.cluster.scheduler.api.event.hbo.jobParams;

import com.bilibili.cluster.scheduler.api.event.AbstractTaskEventHandler;
import com.bilibili.cluster.scheduler.api.service.flow.ExecutionNodePropsService;
import com.bilibili.cluster.scheduler.api.service.hbo.HboService;
import com.bilibili.cluster.scheduler.common.dto.hbo.model.HboJob;
import com.bilibili.cluster.scheduler.common.dto.hbo.model.HboJobInfo;
import com.bilibili.cluster.scheduler.common.dto.hbo.pararms.HboJobParamsDeleteJobExtParams;
import com.bilibili.cluster.scheduler.common.entity.ExecutionNodeEntity;
import com.bilibili.cluster.scheduler.common.enums.NodeExecuteStatusEnum;
import com.bilibili.cluster.scheduler.common.enums.event.EventStatusEnum;
import com.bilibili.cluster.scheduler.common.enums.event.EventTypeEnum;
import com.bilibili.cluster.scheduler.common.event.TaskEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

/**
 * @description:
 * @Date: 2024/12/30 16:28
 * @Author: nizhiqiang
 */
@Slf4j
@Component
public class HboJobParamsDeleteEventHandler extends AbstractTaskEventHandler {

    @Resource
    HboService hboService;

    @Resource
    ExecutionNodePropsService executionNodePropsService;

    @Override
    public boolean executeTaskEvent(TaskEvent taskEvent) throws Exception {
        String jobId = taskEvent.getExecutionNode().getNodeName();
        List<HboJobInfo> hboJobInfoList = hboService.queryJobListByJobId(Arrays.asList(jobId));
        Long nodeId = taskEvent.getExecutionNode().getId();

        HboJobParamsDeleteJobExtParams jobExtParams = executionNodePropsService.queryNodePropsByNodeId(nodeId, HboJobParamsDeleteJobExtParams.class);
        if (Objects.isNull(jobExtParams)) {
            jobExtParams = new HboJobParamsDeleteJobExtParams();
            jobExtParams.setNodeId(nodeId);
        }

        ExecutionNodeEntity executionNode = executionNodeService.getById(nodeId);
        NodeExecuteStatusEnum nodeStatus = executionNode.getNodeStatus();
        if (nodeStatus.isInRollback()) {
            String beforeParams = jobExtParams.getBeforeParams();
            if (StringUtils.isEmpty(beforeParams)) {
                logPersist(taskEvent, "无法找到变更前参数，无法进行回滚");
                return false;
            }

            logPersist(taskEvent, String.format("回滚任务，回滚任务参数为%s", beforeParams));
            HboJob hboJob = new HboJob();
            hboJob.setJobId(jobId);
            hboJob.setParamList(beforeParams);
            List<HboJob> hboJobList = new LinkedList<>();
            hboJobList.add(hboJob);
            hboService.upsertJob(hboJobList);
            logPersist(taskEvent, "回滚成功");
            return true;
        }

        if (CollectionUtils.isEmpty(hboJobInfoList)) {
            logPersist(taskEvent, "任务不存在无需删除，直接跳过");
            taskEvent.setEventStatus(EventStatusEnum.SKIPPED);
            return true;
        }
        HboJobInfo hboJobInfo = hboJobInfoList.get(0);
        String paramList = hboJobInfo.getParamList();
        jobExtParams.setBeforeParams(paramList);
        executionNodePropsService.saveNodeProp(nodeId, jobExtParams);

        logPersist(taskEvent, "before job param is " + paramList);
        logPersist(taskEvent, "try delete job");
        hboService.deleteJob(Arrays.asList(jobId));
        logPersist(taskEvent, "delete job success");
        return true;
    }

    @Override
    public EventTypeEnum getHandleEventType() {
        return EventTypeEnum.HBO_JOB_PARAMS_DELETE;
    }
}
