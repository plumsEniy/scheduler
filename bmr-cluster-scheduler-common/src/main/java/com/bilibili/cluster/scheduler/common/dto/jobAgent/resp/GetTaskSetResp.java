package com.bilibili.cluster.scheduler.common.dto.jobAgent.resp;

import com.bilibili.cluster.scheduler.common.dto.jobAgent.TaskSetData;
import com.bilibili.cluster.scheduler.common.response.BaseResp;
import lombok.Data;

/**
 * @description: taskset结果
 * @Date: 2024/5/16 10:29
 * @Author: nizhiqiang
 */

@Data
public class GetTaskSetResp extends BaseJobAgentResp {
    private TaskSetData data;
}
