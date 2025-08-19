
package com.bilibili.cluster.scheduler.api.registry.service.impl;

import com.bilibili.cluster.scheduler.api.dto.registry.StrategyType;
import com.bilibili.cluster.scheduler.api.registry.service.MasterConnectStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * This strategy will stop the master server, when disconnected
 */
@Service
@ConditionalOnProperty(prefix = "master.registry-disconnect-strategy", name = "strategy", havingValue = "stop", matchIfMissing = true)
public class MasterStopStrategy implements MasterConnectStrategy {

    private final Logger logger = LoggerFactory.getLogger(MasterStopStrategy.class);

   @Resource
    private RegistryClient registryClient;

    @Override
    public void disconnect() {
        registryClient.getStoppable()
                .stop("Master disconnected from registry, will stop myself due to the stop strategy");
    }

    @Override
    public void reconnect() {
        logger.warn("The current connect strategy is stop, so the master will not reconnect to registry");
    }

    @Override
    public StrategyType getStrategyType() {
        return StrategyType.STOP;
    }
}
