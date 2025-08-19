package com.bilibili.cluster.scheduler.common.enums.node;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @description: 任务操作结果
 * @Date: 2024/3/14 19:01
 * @Author: nizhiqiang
 */
@AllArgsConstructor
public enum NodeOperationResult {
    NORMAL("正常执行"),
    JOB_STATUS_SKIP("因运行状态跳过"),
    BLACK_JOB_SKIP("黑名单任务跳过"),
    MAINARGS_SKIP("mainargs跳过"),
    UPDATE_ERROR("更新属性异常"),
    USER_SKIP("用户跳过"),
    PRE_CHECKPOINT_FAIL("前置ck检查失败"),
    CHECKPOINT_FAIL("ck检查失败"),
    RELOAD_ERROR("reload失败"),
    START_ERROR("启动任务失败"),
    JOB_DETAIL_NOT_FOUND("无法找到jobdetail"),
    STOP_OVERTIME("停止超时"),
    CHECK_JOB_START_ERROR("检查任务启动失败"),
    JOB_AGENT_LOST("job-agent服务异常"),
    UNKNOWN("未知错误"),


    SPARK_JOB_VERSION_LOCKED("spark任务版本已锁定"),
    SPARK_PERIPHERY_COMPONENT_VERSION_LOCKED("spark组件已锁定"),
    ;
    @Getter
    private String desc;
}
