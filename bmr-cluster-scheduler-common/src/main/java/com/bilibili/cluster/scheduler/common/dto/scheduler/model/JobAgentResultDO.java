package com.bilibili.cluster.scheduler.common.dto.scheduler.model;

import com.bilibili.cluster.scheduler.common.dto.scheduler.resp.JobAgentResp;
import com.bilibili.cluster.scheduler.common.response.BaseResp;
import lombok.Data;

import java.util.Objects;

@Data
public class JobAgentResultDO extends BaseResp {
    private boolean success;
    private JobAgentTaskSetDO data;

    public JobAgentResultDO(JobAgentResp jobAgentResp) {
        this.setSuccess(jobAgentResp.isSuccess());
        if (success) {
            JData data = jobAgentResp.getData();
            if (Objects.isNull(data)) {
                return;
            }
            this.data = new JobAgentTaskSetDO(data.getId());
        }
    }
}
