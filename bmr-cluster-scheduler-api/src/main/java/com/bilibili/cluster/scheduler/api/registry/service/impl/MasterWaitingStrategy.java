
package com.bilibili.cluster.scheduler.api.registry.service.impl;

import com.bilibili.cluster.scheduler.api.configuration.MasterConfig;
import com.bilibili.cluster.scheduler.api.dto.registry.StrategyType;
import com.bilibili.cluster.scheduler.api.exceptions.RegistryException;
import com.bilibili.cluster.scheduler.api.registry.service.MasterConnectStrategy;
import com.bilibili.cluster.scheduler.common.lifecycle.ServerLifeCycleException;
import com.bilibili.cluster.scheduler.common.lifecycle.ServerLifeCycleManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.Duration;

/**
 * This strategy will change the server status
 */
@Service
@ConditionalOnProperty(prefix = "master.registry-disconnect-strategy", name = "strategy", havingValue = "waiting")
public class MasterWaitingStrategy implements MasterConnectStrategy {

    private final Logger logger = LoggerFactory.getLogger(MasterWaitingStrategy.class);

   @Resource
    private MasterConfig masterConfig;
   @Resource
    private RegistryClient registryClient;

    @Override
    public void disconnect() {
        try {
            ServerLifeCycleManager.toWaiting();
            // todo: clear the current resource
            clearMasterResource();
            Duration maxWaitingTime = masterConfig.getRegistryDisconnectStrategy().getMaxWaitingTime();
            try {
                logger.info("Master disconnect from registry will try to reconnect in {} s",
                        maxWaitingTime.getSeconds());
                registryClient.connectUntilTimeout(maxWaitingTime);
            } catch (RegistryException ex) {
                throw new ServerLifeCycleException(
                        String.format("Waiting to reconnect to registry in %s failed", maxWaitingTime), ex);
            }
        } catch (ServerLifeCycleException e) {
            String errorMessage = String.format(
                    "Disconnect from registry and change the current status to waiting error, the current server state is %s, will stop the current server",
                    ServerLifeCycleManager.getServerStatus());
            logger.error(errorMessage, e);
            registryClient.getStoppable().stop(errorMessage);
        } catch (RegistryException ex) {
            String errorMessage = "Disconnect from registry and waiting to reconnect failed, will stop the server";
            logger.error(errorMessage, ex);
            registryClient.getStoppable().stop(errorMessage);
        } catch (Exception ex) {
            String errorMessage = "Disconnect from registry and get an unknown exception, will stop the server";
            logger.error(errorMessage, ex);
            registryClient.getStoppable().stop(errorMessage);
        }
    }

    @Override
    public void reconnect() {
        try {
            ServerLifeCycleManager.recoverFromWaiting();
            reStartMasterResource();
            logger.info("Recover from waiting success, the current server status is {}",
                    ServerLifeCycleManager.getServerStatus());
        } catch (Exception e) {
            String errorMessage =
                    String.format("Recover from waiting failed, the current server status is %s, will stop the server",
                            ServerLifeCycleManager.getServerStatus());
            logger.error(errorMessage, e);
            registryClient.getStoppable().stop(errorMessage);
        }
    }

    @Override
    public StrategyType getStrategyType() {
        return StrategyType.WAITING;
    }

    private void clearMasterResource() {
        logger.warn("Master clear workflow event queue due to lost registry connection");
    }

    private void reStartMasterResource() {
        // reopen the resource
        logger.warn("ZK reconnect Master reconnect to registry");
    }
}
