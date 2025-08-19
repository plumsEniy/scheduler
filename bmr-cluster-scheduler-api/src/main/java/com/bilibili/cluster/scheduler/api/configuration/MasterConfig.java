
package com.bilibili.cluster.scheduler.api.configuration;

import com.bilibili.cluster.scheduler.api.registry.service.ConnectStrategyProperties;
import com.bilibili.cluster.scheduler.common.Constants;
import com.bilibili.cluster.scheduler.common.utils.NetUtils;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Data
@Validated
@Configuration
@ConfigurationProperties(prefix = "master")
public class MasterConfig implements Validator {

    private Logger logger = LoggerFactory.getLogger(MasterConfig.class);
    /**
     * The master RPC server listen port.
     */
    private int listenPort = 9001;
    /**
     * The max batch size used to fetch command from database.
     */
    private int fetchCommandNum = 30;
    /**
     * The thread number used to prepare processInstance. This number shouldn't bigger than fetchCommandNum.
     */
    private int preExecThreads = 30;
    /**
     * todo: We may need to split the process/task into different thread size.
     * The thread number used to handle processInstance and task event.
     * Will create two thread poll to execute
     */
    private int execThreads = 15;

    /**
     * Master heart beat task execute interval.
     */
    private Duration heartbeatInterval = Duration.ofSeconds(10);

    /**
     * state wheel check interval, if this value is bigger, may increase the delay of task/processInstance.
     */
    private Duration stateWheelInterval = Duration.ofMillis(5);
    private Duration failoverInterval = Duration.ofMinutes(10);
    private ConnectStrategyProperties registryDisconnectStrategy = new ConnectStrategyProperties();

    // ip:listenPort
    private String masterAddress;

    // /nodes/master/ip:listenPort
    private String masterRegistryPath;

    @Override
    public boolean supports(Class<?> clazz) {
        return MasterConfig.class.isAssignableFrom(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        MasterConfig masterConfig = (MasterConfig) target;
        if (masterConfig.getListenPort() <= 0) {
            errors.rejectValue("listen-port", null, "is invalidated");
        }
        if (masterConfig.getFetchCommandNum() <= 0) {
            errors.rejectValue("fetch-command-num", null, "should be a positive value");
        }
        if (masterConfig.getPreExecThreads() <= 0) {
            errors.rejectValue("per-exec-threads", null, "should be a positive value");
        }
        if (masterConfig.getExecThreads() <= 0) {
            errors.rejectValue("exec-threads", null, "should be a positive value");
        }

        if (masterConfig.getHeartbeatInterval().toMillis() < 0) {
            errors.rejectValue("heartbeat-interval", null, "should be a valid duration");
        }

        if (masterConfig.getStateWheelInterval().toMillis() <= 0) {
            errors.rejectValue("state-wheel-interval", null, "should be a valid duration");
        }
        if (masterConfig.getFailoverInterval().toMillis() <= 0) {
            errors.rejectValue("failover-interval", null, "should be a valid duration");
        }

        masterConfig.setMasterAddress(NetUtils.getAddr(masterConfig.getListenPort()));
        masterConfig.setMasterRegistryPath(Constants.REGISTRY_DTS_MASTERS + "/" + masterConfig.getMasterAddress());
        printConfig();
    }

    private void printConfig() {
        logger.info("Master config: listenPort -> {} ", listenPort);
        logger.info("Master config: fetchCommandNum -> {} ", fetchCommandNum);
        logger.info("Master config: preExecThreads -> {} ", preExecThreads);
        logger.info("Master config: execThreads -> {} ", execThreads);
        logger.info("Master config: heartbeatInterval -> {} ", heartbeatInterval);
        logger.info("Master config: stateWheelInterval -> {} ", stateWheelInterval);
        logger.info("Master config: failoverInterval -> {} ", failoverInterval);
        logger.info("Master config: registryDisconnectStrategy -> {} ", registryDisconnectStrategy);
        logger.info("Master config: masterAddress -> {} ", masterAddress);
        logger.info("Master config: masterRegistryPath -> {} ", masterRegistryPath);
    }
}
