package com.bilibili.cluster.scheduler.common.dto.jobAgent;

import cn.hutool.core.annotation.Alias;
import lombok.Data;

import java.util.List;

@Data
public class TaskAtomReport {
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
    @Alias("execDir")
    private String exec_dir;
    @Alias("execUser")
    private String exec_user;
    private String env_string;
    private int timeout;
    @Alias("isSsh")
    private boolean is_ssh;
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
