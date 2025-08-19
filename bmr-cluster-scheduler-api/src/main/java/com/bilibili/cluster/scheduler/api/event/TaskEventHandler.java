package com.bilibili.cluster.scheduler.api.event;


import com.bilibili.cluster.scheduler.api.exceptions.TaskEventHandleException;
import com.bilibili.cluster.scheduler.common.enums.event.EventTypeEnum;
import com.bilibili.cluster.scheduler.common.event.TaskEvent;

public interface TaskEventHandler {

    /**
     * Handle the task event
     *
     * @throws TaskEventHandleException this exception means we need to retry this event
     */
    boolean handleTaskEvent(TaskEvent taskEvent) throws TaskEventHandleException;

    EventTypeEnum getHandleEventType();

}
