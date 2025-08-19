package com.bilibili.cluster.scheduler.common.dto.jobAgent;

import cn.hutool.core.annotation.Alias;
import lombok.Data;

@Data
public class TaskLog {
    private long id;
    @Alias("atom_id")
    private long atomId;
    private String message;
    @Alias("task_type")
    private int taskType;
    private String ctime;
    private String mtime;
}
