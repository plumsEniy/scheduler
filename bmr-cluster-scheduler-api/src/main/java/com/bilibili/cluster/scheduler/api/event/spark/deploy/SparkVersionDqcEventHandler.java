package com.bilibili.cluster.scheduler.api.event.spark.deploy;


import com.bilibili.cluster.scheduler.api.event.AbstractTaskEventHandler;
import com.bilibili.cluster.scheduler.common.enums.event.EventTypeEnum;
import com.bilibili.cluster.scheduler.common.event.TaskEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SparkVersionDqcEventHandler extends AbstractTaskEventHandler {

    @Override
    public boolean executeTaskEvent(TaskEvent taskEvent) throws Exception {
        logPersist(taskEvent, "current not has any dqc rule, will skip dqc...");
        return true;
    }

    @Override
    public EventTypeEnum getHandleEventType() {
        return EventTypeEnum.SPARK_VERSION_DEPLOY_DQC;
    }
}
