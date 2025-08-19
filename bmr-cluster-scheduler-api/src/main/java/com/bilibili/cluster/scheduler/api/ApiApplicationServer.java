package com.bilibili.cluster.scheduler.api;

import com.bilibili.cluster.scheduler.api.bean.SpringApplicationContext;
import com.bilibili.cluster.scheduler.api.registry.service.impl.MasterRegistryClient;
import com.bilibili.cluster.scheduler.api.scheduler.bootstrap.MasterSchedulerBootstrap;
import com.bilibili.cluster.scheduler.api.service.failover.FailoverExecuteThread;
import com.bilibili.cluster.scheduler.common.Constants;
import com.bilibili.cluster.scheduler.common.IStoppable;
import com.bilibili.cluster.scheduler.common.lifecycle.ServerLifeCycleManager;
import com.bilibili.cluster.scheduler.common.utils.ThreadUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import pleiades.venus.starter.boot.ApplicationBootConfiguration;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

@ServletComponentScan
@SpringBootApplication(exclude = {ApplicationBootConfiguration.class})
@EnableScheduling
@EnableTransactionManagement
@EnableAspectJAutoProxy(exposeProxy = true)
@ComponentScan("com.bilibili.cluster.scheduler")
@EnableSwagger2
public class ApiApplicationServer implements IStoppable {

    private final Logger logger = LoggerFactory.getLogger(ApiApplicationServer.class);

    @Resource
    private SpringApplicationContext springApplicationContext;

    @Resource
    private MasterRegistryClient masterRegistryClient;

    @Resource
    private MasterSchedulerBootstrap masterSchedulerBootstrap;

    @Resource
    private FailoverExecuteThread failoverExecuteThread;

    public static void main(String[] args) {
        SpringApplication.run(ApiApplicationServer.class);

    }

    @PostConstruct
    public void run() {
        this.masterRegistryClient.start();
        this.masterRegistryClient.setRegistryStoppable(this);

        this.masterSchedulerBootstrap.init();
        this.masterSchedulerBootstrap.start();
        this.failoverExecuteThread.start();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (!ServerLifeCycleManager.isStopped()) {
                close("master shutdownHook");
            }
        }));
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (!ServerLifeCycleManager.isStopped()) {
                close("master shutdownHook");
            }
        }));
    }

    public void close(String cause) {
        // set stop signal is true
        // execute only once
        if (!ServerLifeCycleManager.toStopped()) {
            logger.warn("MasterServer is already stopped, current cause: {}", cause);
            return;
        }
        // thread sleep 3 seconds for thread quietly stop
        ThreadUtils.sleep(Constants.SERVER_CLOSE_WAIT_TIME.toMillis());
        try (
                MasterRegistryClient closedMasterRegistryClient = masterRegistryClient;
                MasterSchedulerBootstrap closedSchedulerBootstrap = masterSchedulerBootstrap;
                SpringApplicationContext closedSpringContext = springApplicationContext) {
            logger.info("Master server is stopping, current cause : {}", cause);
        } catch (Exception e) {
            logger.error("MasterServer stop failed, current cause: {}", cause, e);
            return;
        }
        logger.info("MasterServer stopped, current cause: {}", cause);
    }

    @Override
    public void stop(String cause) {
        close(cause);
    }
}
