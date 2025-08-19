package com.bilibili.cluster.scheduler.api.event.random;

import com.bilibili.cluster.scheduler.api.event.BatchedTaskEventHandler;
import com.bilibili.cluster.scheduler.common.entity.ExecutionNodeEntity;
import com.bilibili.cluster.scheduler.common.enums.event.EventTypeEnum;
import com.bilibili.cluster.scheduler.common.event.TaskEvent;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class BatchedAlignTestTaskEventHandler extends BatchedTaskEventHandler {

    @Override
    public boolean batchExecEvent(TaskEvent taskEvent, List<ExecutionNodeEntity> nodeEntityList) throws Exception {
        log(taskEvent, "task summary is :" + taskEvent.getSummary());
        Thread.sleep(getMaxLoopStep());
        log(taskEvent, "BatchedAlignTestTaskEventHandler batchExecEvent nodeEntityList is: " + nodeEntityList);
        Thread.sleep(getMaxLoopStep() * 10);
        return true;
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
    public void printLog(TaskEvent taskEvent, String logContent) {

    }

    @Override
    public int logMod() {
        return 3;
    }

    @Override
    public EventTypeEnum getHandleEventType() {
        return EventTypeEnum.BATCH_ALIGN_TEST_EXEC_EVENT;
    }
}
