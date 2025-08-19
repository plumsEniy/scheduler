package com.bilibili.cluster.scheduler.api.event.dolphinScheduler.env;

import com.bilibili.cluster.scheduler.common.event.TaskEvent;

import java.util.Map;

public interface DolphinEnvExtendedService {

    void fillExtendedEnv(TaskEvent taskEvent, Map<String, Object> env);

}
