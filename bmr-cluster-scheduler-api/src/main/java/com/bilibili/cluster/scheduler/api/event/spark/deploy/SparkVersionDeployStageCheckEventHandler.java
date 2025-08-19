package com.bilibili.cluster.scheduler.api.event.spark.deploy;

import com.bilibili.cluster.scheduler.api.event.BatchedTaskEventHandler;
import com.bilibili.cluster.scheduler.common.entity.ExecutionNodeEntity;
import com.bilibili.cluster.scheduler.common.enums.event.EventTypeEnum;
import com.bilibili.cluster.scheduler.common.event.TaskEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class SparkVersionDeployStageCheckEventHandler extends BatchedTaskEventHandler {

    @Override
    public boolean batchExecEvent(TaskEvent taskEvent, List<ExecutionNodeEntity> nodeEntityList) throws Exception {
        // todo: check stage deployed jobs
        return true;
    }

    @Override
    public int getMinLoopWait() {
        return 10_000;
    }

    @Override
    public int getMaxLoopStep() {
        return 60_000;
    }

    @Override
    public void printLog(TaskEvent taskEvent, String logContent) {
        log.info(logContent);
    }

    @Override
    public int logMod() {
        return 10;
    }

    @Override
    public EventTypeEnum getHandleEventType() {
        return EventTypeEnum.SPARK_VERSION_STAGE_CHECK_EXEC_EVENT;
    }
}
