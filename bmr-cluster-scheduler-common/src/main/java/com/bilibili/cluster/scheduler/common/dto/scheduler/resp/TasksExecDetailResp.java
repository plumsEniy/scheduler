package com.bilibili.cluster.scheduler.common.dto.scheduler.resp;

import com.bilibili.cluster.scheduler.common.dto.scheduler.model.TasksExecDetailData;
import lombok.Data;

@Data
public class TasksExecDetailResp extends BaseDolphinSchedulerResp {

    private TasksExecDetailData data;

}
