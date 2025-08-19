
package com.bilibili.cluster.scheduler.api.registry.service.queue;


import com.bilibili.cluster.scheduler.api.exceptions.TaskPriorityQueueException;

import java.util.concurrent.TimeUnit;

/**
 * task priority queue
 *
 * @param <T>
 */
public interface TaskPriorityQueue<T> {

    /**
     * put task info
     *
     * @param taskInfo taskInfo
     * @throws TaskPriorityQueueException
     */
    void put(T taskInfo);

    /**
     * take taskInfo
     *
     * @return taskInfo
     * @throws TaskPriorityQueueException
     */
    T take() throws TaskPriorityQueueException, InterruptedException;

    /**
     * poll taskInfo with timeout
     *
     * @param timeout
     * @param unit
     * @return
     * @throws TaskPriorityQueueException
     * @throws InterruptedException
     */
    T poll(long timeout, TimeUnit unit) throws TaskPriorityQueueException, InterruptedException;

    /**
     * size
     *
     * @return size
     * @throws TaskPriorityQueueException
     */
    int size() throws TaskPriorityQueueException;
}
