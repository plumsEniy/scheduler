
package com.bilibili.cluster.scheduler.api.registry.zookeeper;

import com.bilibili.cluster.scheduler.api.registry.service.ConnectionListener;
import com.bilibili.cluster.scheduler.api.registry.service.ConnectionState;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.state.ConnectionStateListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ZookeeperConnectionStateListener implements ConnectionStateListener {

    private static final Logger logger = LoggerFactory.getLogger(ZookeeperConnectionStateListener.class);

    private final ConnectionListener listener;

    public ZookeeperConnectionStateListener(ConnectionListener listener) {
        this.listener = listener;
    }

    @Override
    public void stateChanged(CuratorFramework client,
                             org.apache.curator.framework.state.ConnectionState newState) {
        switch (newState) {
            case LOST:
                logger.warn("Registry disconnected");
                listener.onUpdate(ConnectionState.DISCONNECTED);
                break;
            case RECONNECTED:
                logger.info("Registry reconnected");
                listener.onUpdate(ConnectionState.RECONNECTED);
                break;
            case SUSPENDED:
                logger.warn("Registry suspended");
                listener.onUpdate(ConnectionState.SUSPENDED);
                break;
            default:
                break;
        }
    }
}
