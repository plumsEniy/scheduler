package com.bilibili.cluster.scheduler.api.event.random;

import com.bilibili.cluster.scheduler.api.event.AbstractTaskEventHandler;
import com.bilibili.cluster.scheduler.common.Constants;
import com.bilibili.cluster.scheduler.common.enums.event.EventTypeEnum;
import com.bilibili.cluster.scheduler.common.event.TaskEvent;
import org.springframework.stereotype.Component;

@Component
public class RandomTestTaskEventHandler extends AbstractTaskEventHandler {
    @Override
    public boolean executeTaskEvent(TaskEvent taskEvent) throws Exception {
        int randomNum = taskEvent.getRandom().nextInt(100);
        Thread.sleep(randomNum * Constants.ONE_SECOND);
        logPersist(taskEvent,
                "RandomTestTaskEventHandler get random number is: " + randomNum);
        if (randomNum % 2 == 0 || randomNum % 3 == 0) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public EventTypeEnum getHandleEventType() {
        return EventTypeEnum.RANDOM_TEST_EXEC_EVENT;
    }
}
