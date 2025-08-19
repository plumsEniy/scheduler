package com.bilibili.cluster.scheduler.api.event.spark.deploy;


import com.bilibili.cluster.scheduler.api.event.AbstractTaskEventHandler;
import com.bilibili.cluster.scheduler.common.enums.event.EventTypeEnum;
import com.bilibili.cluster.scheduler.common.event.TaskEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SparkVersionPostCheckEventHandler extends AbstractTaskEventHandler {

    @Override
    public boolean executeTaskEvent(TaskEvent taskEvent) throws Exception {
        logPersist(taskEvent, "current not has any post check rule, will skip post check...");
        return true;
    }

    @Override
    public EventTypeEnum getHandleEventType() {
        return EventTypeEnum.SPARK_VERSION_DEPLOY_POST_CHECK;
    }
}
