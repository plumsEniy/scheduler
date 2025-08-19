package com.bilibili.cluster.scheduler.common.dto.jobAgent.model;

import com.bilibili.cluster.scheduler.common.enums.jobAgent.ScriptJobExecuteStateEnum;
import lombok.Data;
import lombok.ToString;

/**
 * @description: 脚本执行结果
 * @Date: 2023/9/13 19:58
 * @Author: nizhiqiang
 */
@Data
@ToString
public class JobResult {
    private ScriptJobExecuteStateEnum state;
    private String log;
    private Integer setId;
}
