package com.bilibili.cluster.scheduler.common.dto.jobAgent;

import cn.hutool.core.annotation.Alias;
import lombok.Data;

import java.util.List;

/**
 * @description: 任务atom
 * @Date: 2024/5/10 10:59
 * @Author: nizhiqiang
 */
@Data
public class TaskAtomDetail {
    private long id;
    @Alias("set_id")
    private long setId;
    @Alias("flow_id")
    private long flowId;
    @Alias("script_id")
    private long scriptId;
    private String hostname;
    private String uuid;
    private String args;
    @Alias("exec_dir")
    private String execDir;
    @Alias("exec_user")
    private String execUser;
    private int timeout;
    @Alias("is_ssh")
    private boolean isSsh;
    @Alias("jump_agent")
    private String jumpAgent;
    @Alias("cost_timestamp")
    private long costTimestamp;
    private int state;
    @Alias("created_by")
    private String createdBy;
    private String stime;
    private String etime;
    private String ctime;
    private String mtime;
    @Alias("start_time")
    private long startTime;
    @Alias("end_time")
    private long endTime;
    @Alias("task_log")
    private List<TaskLog> taskLog;
}
