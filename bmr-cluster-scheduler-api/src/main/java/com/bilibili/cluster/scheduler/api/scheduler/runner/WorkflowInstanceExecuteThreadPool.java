package com.bilibili.cluster.scheduler.api.scheduler.runner;

import com.bilibili.cluster.scheduler.api.configuration.MasterConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Used to execute {@link WorkflowInstanceExecuteRunnable}.
 */
@Component
@Slf4j
public class WorkflowInstanceExecuteThreadPool extends ThreadPoolTaskExecutor {

    @Resource
    private MasterConfig masterConfig;


    /**
     * multi-thread filter, avoid handling workflow at the same time
     */
    private ConcurrentHashMap<String, WorkflowInstanceExecuteRunnable> multiThreadFilterMap = new ConcurrentHashMap<>();

    @PostConstruct
    private void init() {
        this.setDaemon(true);
        this.setThreadNamePrefix("WorkflowInstanceExecuteThread-");
        this.setMaxPoolSize(masterConfig.getExecThreads());
        this.setCorePoolSize(masterConfig.getExecThreads());
    }

}
