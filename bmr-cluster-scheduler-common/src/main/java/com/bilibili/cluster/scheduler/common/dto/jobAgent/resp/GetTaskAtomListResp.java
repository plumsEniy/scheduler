package com.bilibili.cluster.scheduler.common.dto.jobAgent.resp;

import com.bilibili.cluster.scheduler.common.dto.jobAgent.model.TaskAtomListData;
import lombok.Data;

/**
 * @description: 任务atom
 * @Date: 2024/5/16 10:41
 * @Author: nizhiqiang
 */
@Data
public class GetTaskAtomListResp extends BaseJobAgentResp{
    private TaskAtomListData data;

}
