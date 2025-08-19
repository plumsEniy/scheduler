package com.bilibili.cluster.scheduler.api.event.spark.client;

import com.bilibili.cluster.scheduler.api.event.dolphinScheduler.AbstractDolphinSchedulerEventHandler;
import com.bilibili.cluster.scheduler.common.Constants;
import com.bilibili.cluster.scheduler.common.enums.event.EventTypeEnum;
import com.bilibili.cluster.scheduler.common.event.TaskEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

@Slf4j
@Component
public class SparkClientPackCleanEventHandler extends AbstractDolphinSchedulerEventHandler {

    @Override
    protected Map<String, Object> getDolphinExecuteEnv(TaskEvent taskEvent, List<String> hostList) {
        Map<String, Object> evnMap = new HashMap<>();
        StringJoiner joiner = new StringJoiner(Constants.COMMA);
        hostList.forEach(joiner::add);
        // 机器列表
        String hosts = joiner.toString();
        evnMap.put(Constants.SYSTEM_JOBAGENT_EXEC_HOSTS, hosts);
        logPersist(taskEvent, "execute hosts is: \n" + hosts);
        return evnMap;
    }

    @Override
    public int getMinLoopWait() {
        return 3_000;
    }

    @Override
    public int getMaxLoopStep() {
        return 5_000;
    }

    @Override
    public EventTypeEnum getHandleEventType() {
        return EventTypeEnum.SPARK_CLIENT_PACK_CLEAN_EVENT;
    }
}
