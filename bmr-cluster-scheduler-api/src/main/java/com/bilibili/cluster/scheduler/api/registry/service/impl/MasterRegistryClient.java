package com.bilibili.cluster.scheduler.api.registry.service.impl;

import cn.hutool.json.JSONUtil;
import com.bilibili.cluster.scheduler.api.configuration.MasterConfig;
import com.bilibili.cluster.scheduler.api.dto.registry.NodeType;
import com.bilibili.cluster.scheduler.api.exceptions.RegistryException;
import com.bilibili.cluster.scheduler.api.registry.heart.task.MasterHeartBeatTask;
import com.bilibili.cluster.scheduler.api.registry.service.MasterConnectStrategy;
import com.bilibili.cluster.scheduler.api.service.failover.FailoverService;
import com.bilibili.cluster.scheduler.common.Constants;
import com.bilibili.cluster.scheduler.common.IStoppable;
import com.bilibili.cluster.scheduler.common.utils.NetUtils;
import com.bilibili.cluster.scheduler.common.utils.ThreadUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * zk注册
 */
@Component
public class MasterRegistryClient implements AutoCloseable {

    private static final Logger logger = LoggerFactory.getLogger(MasterRegistryClient.class);

    @Resource
    private FailoverService failoverService;

    @Resource
    private RegistryClient registryClient;

    @Resource
    private MasterConfig masterConfig;

    @Resource
    private MasterConnectStrategy masterConnectStrategy;

    private MasterHeartBeatTask masterHeartBeatTask;

    public void start() {
        try {
            this.masterHeartBeatTask = new MasterHeartBeatTask(masterConfig, registryClient);
            // master registry
            registry();
            registryClient.addConnectionStateListener(new MasterConnectionStateListener(masterConfig, registryClient, masterConnectStrategy));
            registryClient.subscribe(Constants.REGISTRY_DTS_NODE, new MasterRegistryDataListener());
        } catch (Exception e) {
            throw new RegistryException("Master registry client start up error", e);
        }
    }

    public void setRegistryStoppable(IStoppable stoppable) {
        registryClient.setStoppable(stoppable);
    }

    @Override
    public void close() {
        // TODO unsubscribe MasterRegistryDataListener
        deregister();
    }

    /**
     * remove master node path
     *
     * @param path     node path
     * @param nodeType node type
     * @param failover is failover
     */
    public void removeMasterNodePath(String path, NodeType nodeType, boolean failover) {
        logger.info("{} node deleted : {}", nodeType, path);

        if (StringUtils.isEmpty(path)) {
            logger.error("server down error: empty path: {}, nodeType:{}", path, nodeType);
            return;
        }

        String serverHost = registryClient.getHostByEventDataPath(path);
        if (StringUtils.isEmpty(serverHost)) {
            logger.error("server down error: unknown path: {}, nodeType:{}", path, nodeType);
            return;
        }

        try {
            if (!registryClient.exists(path)) {
                logger.info("path: {} not exists", path);
            }
            // failover server
            if (failover) {
                failoverService.failoverServerWhenDown(serverHost, nodeType);
            }
        } catch (Exception e) {
            logger.error("{} server failover failed, host:{}", nodeType, serverHost, e);
        }
    }

    /**
     * Registry the current master server itself to registry.
     */
    void registry() {
        logger.info("Master node : {} registering to registry center", masterConfig.getMasterAddress());
        String masterRegistryPath = masterConfig.getMasterRegistryPath();
        logger.info("Master masterRegistryPath : {} ", masterRegistryPath);

        // remove before persist
        registryClient.remove(masterRegistryPath);
        registryClient.persistEphemeral(masterRegistryPath, JSONUtil.toJsonStr(masterHeartBeatTask.getHeartBeat()));

        while (!registryClient.checkNodeExists(NetUtils.getHost(), NodeType.MASTER)) {
            logger.warn("The current master server node:{} cannot find in registry", NetUtils.getHost());
            ThreadUtils.sleep(Constants.SLEEP_TIME_MILLIS);
        }

        // sleep 1s, waiting master failover remove
        ThreadUtils.sleep(Constants.SLEEP_TIME_MILLIS);
        masterHeartBeatTask.start();
        logger.info("Master node : {} registered to registry center successfully", masterConfig.getMasterAddress());

    }

    public void deregister() {
        try {
            registryClient.remove(masterConfig.getMasterRegistryPath());
            logger.info("Master node : {} unRegistry to register center.", masterConfig.getMasterAddress());
            if (masterHeartBeatTask != null) {
                masterHeartBeatTask.shutdown();
            }
            registryClient.close();
        } catch (Exception e) {
            logger.error("MasterServer remove registry path exception ", e);
        }
    }

}
