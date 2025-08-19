package com.bilibili.cluster.scheduler.api.service.failover;

import com.bilibili.cluster.scheduler.api.dto.registry.NodeType;
import lombok.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * failover service
 */
@Component
public class FailoverService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FailoverService.class);

    private final MasterFailoverService masterFailoverService;

    public FailoverService(@NonNull MasterFailoverService masterFailoverService) {
        this.masterFailoverService = masterFailoverService;
    }

    /**
     * failover server when server down
     *
     * @param serverHost server host
     * @param nodeType   node type
     */
    public void failoverServerWhenDown(String serverHost, NodeType nodeType) {
        switch (nodeType) {
            case MASTER:
                LOGGER.info("Master failover starting, masterServer: {}", serverHost);
                masterFailoverService.failoverMaster(serverHost);
                LOGGER.info("Master failover finished, masterServer: {}", serverHost);
                break;
            default:
                break;
        }
    }

}
