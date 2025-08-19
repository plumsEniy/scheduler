
package com.bilibili.cluster.scheduler.api.registry.service.impl;

import com.bilibili.cluster.scheduler.api.configuration.MasterConfig;
import com.bilibili.cluster.scheduler.api.dto.registry.NodeType;
import com.bilibili.cluster.scheduler.api.dto.registry.RegistryEvent;
import com.bilibili.cluster.scheduler.api.dto.registry.Server;
import com.bilibili.cluster.scheduler.api.registry.service.SubscribeListener;
import com.bilibili.cluster.scheduler.api.registry.service.queue.MasterPriorityQueue;
import com.bilibili.cluster.scheduler.common.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * server node manager
 */
@Service
public class ServerNodeManager implements InitializingBean {

    private final Logger logger = LoggerFactory.getLogger(ServerNodeManager.class);

    private final Lock masterLock = new ReentrantLock();

    /**
     * master nodes
     */
    private final Set<String> masterNodes = new HashSet<>();

    @Resource
    private RegistryClient registryClient;

    private final MasterPriorityQueue masterPriorityQueue = new MasterPriorityQueue();

    /**
     * master config
     */
    @Resource
    private MasterConfig masterConfig;

    private static volatile int MASTER_SLOT = 0;

    private static volatile int MASTER_SIZE = 0;

    public static int getSlot() {
        return MASTER_SLOT;
    }

    public static int getMasterSize() {
        return MASTER_SIZE;
    }

    /**
     * init listener
     *
     * @throws Exception if error throws Exception
     */
    @Override
    public void afterPropertiesSet() throws Exception {

        // load nodes from zookeeper
        load();

        // init MasterNodeListener listener
        registryClient.subscribe(Constants.REGISTRY_DTS_MASTERS, new MasterDataListener());

    }

    /**
     * load nodes from zookeeper
     */
    public void load() {
        // master nodes from zookeeper
        updateMasterNodes();
    }

    class MasterDataListener implements SubscribeListener {

        @Override
        public void notify(RegistryEvent event) {
            final String path = event.path();
            final RegistryEvent.Type type = event.type();
            if (registryClient.isMasterPath(path)) {
                try {
                    if (type.equals(RegistryEvent.Type.ADD)) {
                        logger.info("master node : {} added.", path);
                        updateMasterNodes();
                    }
                    if (type.equals(RegistryEvent.Type.REMOVE)) {
                        logger.info("master node : {} down.", path);
                        updateMasterNodes();
                    }
                } catch (Exception ex) {
                    logger.error("MasterNodeListener capture data change and get data failed.", ex);
                }
            }
        }
    }

    private void updateMasterNodes() {
        MASTER_SLOT = 0;
        MASTER_SIZE = 0;
        this.masterNodes.clear();
        String nodeLock = Constants.REGISTRY_DTS_LOCK_MASTERS;
        try {
            registryClient.getLock(nodeLock);
            Collection<String> currentNodes = registryClient.getMasterNodesDirectly();
            List<Server> masterNodes = registryClient.getServerList(NodeType.MASTER);
            syncMasterNodes(currentNodes, masterNodes);
        } catch (Exception e) {
            logger.error("update master nodes error", e);
        } finally {
            registryClient.releaseLock(nodeLock);
        }

    }

    /**
     * sync master nodes
     *
     * @param nodes master nodes
     */
    private void syncMasterNodes(Collection<String> nodes, List<Server> masterNodes) {
        masterLock.lock();
        try {
            this.masterNodes.addAll(nodes);
            this.masterPriorityQueue.clear();
            this.masterPriorityQueue.putList(masterNodes);
            int index = masterPriorityQueue.getIndex(masterConfig.getMasterAddress());
            if (index >= 0) {
                MASTER_SIZE = nodes.size();
                MASTER_SLOT = index;
            } else {
                logger.warn("current addr:{} is not in active master list",
                        masterConfig.getMasterAddress());
            }
            logger.info("update master nodes, master size: {}, slot: {}, addr: {}", MASTER_SIZE,
                    MASTER_SLOT, masterConfig.getMasterAddress());
        } finally {
            masterLock.unlock();
        }
    }

}
