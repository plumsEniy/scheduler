package com.bilibili.cluster.scheduler.common.dto.jobAgent.resp;

import com.bilibili.cluster.scheduler.common.dto.jobAgent.JobAgentData;
import lombok.Data;

@Data
public class JobAgentCheckResponse extends BaseJobAgentResp {
    private JobAgentData data;
}
