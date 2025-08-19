package com.bilibili.cluster.scheduler.common.dto.scheduler.req;

import cn.hutool.json.JSONUtil;
import com.bilibili.cluster.scheduler.common.enums.scheduler.DolpFailureStrategy;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Map;

/**
 * @description: 开始实例请求
 * @Date: 2024/5/13 20:08
 * @Author: nizhiqiang
 */

@Data
public class StartPipelineReq {

    private long processDefinitionCode;
    private DolpFailureStrategy failureStrategy;
    private String processInstancePriority;
    private String scheduleTime;
    private String warningType;
    private String startParams;

    public StartPipelineReq(String pipelineId, Map<String, Object> execEnv, DolpFailureStrategy failureStrategy) {
        this.failureStrategy = failureStrategy;
        this.processInstancePriority = "MEDIUM";
        this.scheduleTime = "";
        this.warningType = "NONE";
        this.processDefinitionCode = Long.parseLong(pipelineId);
        this.startParams = JSONUtil.toJsonStr(execEnv);
    }
}
