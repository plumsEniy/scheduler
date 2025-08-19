package com.bilibili.cluster.scheduler.common.dto.scheduler;

import com.bilibili.cluster.scheduler.common.dto.scheduler.model.JobAgentResultDO;
import com.bilibili.cluster.scheduler.common.dto.scheduler.model.TaskParamDO;
import com.bilibili.cluster.scheduler.common.enums.dolphin.DolphinWorkflowExecutionStatus;
import com.bilibili.cluster.scheduler.common.enums.scheduler.DolpTaskType;
import lombok.Data;

/**
 * @description: 任务实例详情
 * @Date: 2024/5/13 17:43
 * @Author: nizhiqiang
 */
@Data
public class TaskInstanceDetail {

    private String taskCode;
    private String name;
    private DolphinWorkflowExecutionStatus state;
    private String startTime;
    private String endTime;
    private String host;

    private DolpTaskType taskType;

    private TaskParamDO taskParamDO;
    private JobAgentResultDO jobAgentResultDO;

    private boolean taskComplete;

}
