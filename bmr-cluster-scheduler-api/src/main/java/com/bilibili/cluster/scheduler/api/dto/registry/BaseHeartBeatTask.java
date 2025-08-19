
package com.bilibili.cluster.scheduler.api.dto.registry;

import com.bilibili.cluster.scheduler.common.thread.BaseDaemonThread;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class BaseHeartBeatTask<T> extends BaseDaemonThread {

    private final String threadName;
    private final long heartBeatInterval;

    protected boolean runningFlag;

    public BaseHeartBeatTask(String threadName, long heartBeatInterval) {
        super(threadName);
        this.threadName = threadName;
        this.heartBeatInterval = heartBeatInterval;
        this.runningFlag = true;
    }

    @Override
    public synchronized void start() {
        log.info("Starting {}", threadName);
        super.start();
        log.info("Started {}, heartBeatInterval: {}", threadName, heartBeatInterval);
    }

    @Override
    public void run() {
        while (runningFlag) {
            try {
                T heartBeat = getHeartBeat();
                writeHeartBeat(heartBeat);
            } catch (Exception ex) {
                log.error("{} task execute failed", threadName, ex);
            } finally {
                try {
                    Thread.sleep(heartBeatInterval);
                } catch (InterruptedException e) {
                    handleInterruptException(e);
                }
            }
        }
    }

    public void shutdown() {
        log.warn("{} task finished", threadName);
        runningFlag = false;
    }

    private void handleInterruptException(InterruptedException ex) {
        log.warn("{} has been interrupted", threadName, ex);
        Thread.currentThread().interrupt();
    }

    public abstract T getHeartBeat();

    public abstract void writeHeartBeat(T heartBeat);
}
