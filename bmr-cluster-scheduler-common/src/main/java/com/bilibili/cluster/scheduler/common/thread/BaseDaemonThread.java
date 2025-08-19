
package com.bilibili.cluster.scheduler.common.thread;

/**
 * All thread used in  should extend with this class to avoid the server hang issue.
 */
public abstract class BaseDaemonThread extends Thread {

    protected BaseDaemonThread(Runnable runnable) {
        super(runnable);
        this.setDaemon(true);
    }

    protected BaseDaemonThread(String threadName) {
        super();
        this.setName(threadName);
        this.setDaemon(true);
    }

}
