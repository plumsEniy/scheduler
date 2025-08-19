package com.bilibili.cluster.scheduler.common.dto.scheduler.model;

import lombok.Data;

import java.util.List;

@Data
public class TasksExecDetailData {

    private List<TaskInstance> taskList;

}
