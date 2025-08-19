package com.bilibili.cluster.scheduler.common.dto.oa;

import lombok.Data;

import java.util.List;

/**
 * @description: 提交oa表格
 * @Date: 2024/3/6 15:43
 * @Author: nizhiqiang
 */

@Data
public class SubmitOAFormData<T> {

    private SubmitArgs<T> args;
    private CurrentTask currentTask;
    private List<NextTask> nextTasks;

    @Data
    public static class CurrentTask {
        private String displayName;
        private String parentTaskId;
        private String taskId;
        private String taskName;
    }

    public static class NextTask {

        private List<String> actors;
        private String createTime;
        private String displayName;
        private int performType;
        private String taskId;
        private String taskName;


    }
}
