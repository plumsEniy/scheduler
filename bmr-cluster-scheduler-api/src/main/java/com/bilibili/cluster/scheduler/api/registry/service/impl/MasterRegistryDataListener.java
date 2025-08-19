package com.bilibili.cluster.scheduler.api.registry.service.impl;

import com.bilibili.cluster.scheduler.api.bean.SpringApplicationContext;
import com.bilibili.cluster.scheduler.api.dto.registry.NodeType;
import com.bilibili.cluster.scheduler.api.dto.registry.RegistryEvent;
import com.bilibili.cluster.scheduler.api.registry.service.SubscribeListener;
import com.bilibili.cluster.scheduler.common.Constants;
import com.google.common.base.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MasterRegistryDataListener implements SubscribeListener {

    private static final Logger logger = LoggerFactory.getLogger(MasterRegistryDataListener.class);

    private final MasterRegistryClient masterRegistryClient;

    public MasterRegistryDataListener() {
        masterRegistryClient = SpringApplicationContext.getBean(MasterRegistryClient.class);
    }

    @Override
    public void notify(RegistryEvent event) {
        final String path = event.path();
        if (Strings.isNullOrEmpty(path)) {
            return;
        }
        // monitor master
        if (path.startsWith(Constants.REGISTRY_DTS_MASTERS + Constants.SINGLE_SLASH)) {
            handleMasterEvent(event);
        }
    }

    private void handleMasterEvent(RegistryEvent event) {
        final String path = event.path();
        switch (event.type()) {
            case ADD:
                logger.info("master node added : {}", path);
                break;
            case REMOVE:
                masterRegistryClient.removeMasterNodePath(path, NodeType.MASTER, true);
                break;
            default:
                break;
        }
    }

}
