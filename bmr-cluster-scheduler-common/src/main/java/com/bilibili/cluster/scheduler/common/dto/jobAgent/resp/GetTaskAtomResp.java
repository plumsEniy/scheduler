package com.bilibili.cluster.scheduler.common.dto.jobAgent.resp;

import com.bilibili.cluster.scheduler.common.dto.jobAgent.TaskAtomReport;
import lombok.Data;

@Data
public class GetTaskAtomResp  extends BaseJobAgentResp {

    private TaskAtomReport data;

}
