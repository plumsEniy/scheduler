package com.bilibili.cluster.scheduler.common.dto.scheduler;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.json.JSONUtil;
import com.bilibili.cluster.scheduler.common.dto.scheduler.model.*;
import com.bilibili.cluster.scheduler.common.dto.scheduler.resp.JobAgentResp;
import com.bilibili.cluster.scheduler.common.dto.scheduler.resp.SchedInstanceResp;
import com.bilibili.cluster.scheduler.common.enums.scheduler.DolpFailureStrategy;
import com.bilibili.cluster.scheduler.common.enums.scheduler.WorkflowExecutionStatus;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * @description: 实例详情
 * @Date: 2024/5/13 17:40
 * @Author: nizhiqiang
 */

@Data
public class ExecutionInstanceDetail {

    private int processInstanceId;

    private String globalParams;

    private String state;

    private DolpFailureStrategy failureStrategy;

    private List<TaskInstanceDetail> taskInstanceList;

    //若响应报文code为10116，则证明已调用startPipeline，但是process还没有完全跑起来（因为其是异步操作）
    private boolean isAlreadyExec;

    //
    private boolean success;

    //                            失败策略为失败终止并且正在执行的实例不认为是失败
    public boolean isRealFail() {
        if (failureStrategy == DolpFailureStrategy.END && !WorkflowExecutionStatus.isFailure(state)) {
            return false;
        }
        return true;
    }

    public ExecutionInstanceDetail(SchedInstanceResp resp) {
        if (resp.getCode() == 10116) {
            this.isAlreadyExec = false;
            this.success = true;
            return;
        }
        SchedInstanceData data = resp.getData();

        this.processInstanceId = Integer.parseInt(data.getId());
        this.isAlreadyExec = true;
        this.globalParams = data.getGlobalParams();
        this.state = data.getState();
        if (!StringUtils.isBlank(data.getFailureStrategy())) {
            this.failureStrategy = DolpFailureStrategy.valueOf(data.getFailureStrategy());
        }

        try {
            this.taskInstanceList = setTaskInstanceList(data.getTaskInstanceList());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    private List<TaskInstanceDetail> setTaskInstanceList(List<TaskInstance> taskInstanceLists) throws Exception {
        if (CollUtil.isEmpty(taskInstanceLists)) return Collections.emptyList();
        List<TaskInstanceDetail> resTaskInstanceList = new LinkedList<>();
        for (TaskInstance til : taskInstanceLists) {
            TaskInstanceDetail tid = new TaskInstanceDetail();

            tid.setTaskCode(til.getTaskCode());
            tid.setName(til.getName());
            tid.setState(til.getState());
            tid.setStartTime(til.getStartTime());
            tid.setEndTime(til.getEndTime());
            tid.setHost(til.getHost());
            tid.setTaskType(til.getTaskType());

            if (til.getTaskParams() != null) {
                TaskParaObj taskParaObj = JSONUtil.toBean(til.getTaskParams(), TaskParaObj.class);
                tid.setTaskParamDO(new TaskParamDO(taskParaObj.getRawScript()));
            }

            if (til.getJobAgentResult() != null) {
                JobAgentResp jobAgentResp = JSONUtil.toBean(til.getJobAgentResult(), JobAgentResp.class);
                tid.setJobAgentResultDO(new JobAgentResultDO(jobAgentResp));
            }

            tid.setTaskComplete(til.isTaskComplete());

            resTaskInstanceList.add(tid);
        }
        return resTaskInstanceList;
    }

}
