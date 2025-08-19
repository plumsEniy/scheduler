package com.bilibili.cluster.scheduler.api.registry.service.impl;

import cn.hutool.json.JSONUtil;
import com.bilibili.cluster.scheduler.api.dto.registry.MasterHeartBeat;
import com.bilibili.cluster.scheduler.api.dto.registry.NodeType;
import com.bilibili.cluster.scheduler.api.dto.registry.Server;
import com.bilibili.cluster.scheduler.api.exceptions.RegistryException;
import com.bilibili.cluster.scheduler.api.registry.service.ConnectionListener;
import com.bilibili.cluster.scheduler.api.registry.service.Registry;
import com.bilibili.cluster.scheduler.api.registry.service.SubscribeListener;
import com.bilibili.cluster.scheduler.common.IStoppable;
import com.google.common.base.Strings;
import lombok.NonNull;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import com.bilibili.cluster.scheduler.common.Constants;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;

@Component
public class RegistryClient {

    private static final Logger logger = LoggerFactory.getLogger(RegistryClient.class);

    private static final String EMPTY = "";
    private IStoppable stoppable;

    private final Registry registry;

    public RegistryClient(Registry registry) {
        this.registry = registry;
    }

    @PostConstruct
    public void afterConstruct() {
        initNodes();
    }

    public void connectUntilTimeout(@NonNull Duration duration) throws RegistryException {
        registry.connectUntilTimeout(duration);
    }

    public int getActiveMasterNum() {
        Collection<String> childrenList = new ArrayList<>();
        try {
            // read master node parent path from conf
            if (exists(rootNodePath(NodeType.MASTER))) {
                childrenList = getChildrenKeys(rootNodePath(NodeType.MASTER));
            }
        } catch (Exception e) {
            logger.error("getActiveMasterNum error", e);
        }
        return childrenList.size();
    }

    public List<Server> getServerList(NodeType nodeType) {
        Map<String, String> serverMaps = getServerMaps(nodeType, false);
        String parentPath = rootNodePath(nodeType);

        List<Server> serverList = new ArrayList<>();
        for (Map.Entry<String, String> entry : serverMaps.entrySet()) {
            String serverPath = entry.getKey();
            String heartBeatJson = entry.getValue();
            if (StringUtils.isEmpty(heartBeatJson)) {
                logger.error("The heartBeatJson is empty, serverPath: {}", serverPath);
                continue;
            }
            Server server = new Server();
            switch (nodeType) {
                case MASTER:
                    MasterHeartBeat masterHeartBeat = JSONUtil.toBean(heartBeatJson, MasterHeartBeat.class);
                    server.setCreateTime(new Date(masterHeartBeat.getStartupTime()));
                    server.setLastHeartbeatTime(new Date(masterHeartBeat.getReportTime()));
                    server.setId(masterHeartBeat.getProcessId());
                    break;
            }

            server.setResInfo(heartBeatJson);
            // todo: add host, port in heartBeat Info, so that we don't need to parse this again
            server.setZkDirectory(parentPath + "/" + serverPath);
            // set host and port
            String[] hostAndPort = serverPath.split(Constants.COLON);
            String[] hosts = hostAndPort[0].split(Constants.DIVISION_STRING);
            // fetch the last one
            server.setHost(hosts[hosts.length - 1]);
            server.setPort(Integer.parseInt(hostAndPort[1]));
            serverList.add(server);
        }
        return serverList;
    }

    public Map<String, String> getServerMaps(NodeType nodeType, boolean hostOnly) {
        Map<String, String> serverMap = new HashMap<>();
        try {
            String path = rootNodePath(nodeType);
            Collection<String> serverList = getServerNodes(nodeType);
            for (String server : serverList) {
                String host = server;
                serverMap.putIfAbsent(host, get(path + Constants.SINGLE_SLASH + server));
            }
        } catch (Exception e) {
            logger.error("get server list failed", e);
        }

        return serverMap;
    }

    public boolean checkNodeExists(String host, NodeType nodeType) {
        return getServerMaps(nodeType, true).keySet()
                .stream()
                .anyMatch(it -> it.contains(host));
    }

    public Collection<String> getMasterNodesDirectly() {
        return getChildrenKeys(Constants.REGISTRY_DTS_MASTERS);
    }

    /**
     * get host ip:port, path format: parentPath/ip:port
     *
     * @param path path
     * @return host ip:port, string format: parentPath/ip:port
     */
    public String getHostByEventDataPath(String path) {
        checkArgument(!Strings.isNullOrEmpty(path), "path cannot be null or empty");

        final String[] pathArray = path.split(Constants.SINGLE_SLASH);

        checkArgument(pathArray.length >= 1, "cannot parse path: %s", path);

        return pathArray[pathArray.length - 1];
    }

    public void close() throws IOException {
        registry.close();
    }

    public void persistEphemeral(String key, String value) {
        registry.put(key, value, true);
    }

    public void remove(String key) {
        registry.delete(key);
    }

    public String get(String key) {
        return registry.get(key);
    }

    public void subscribe(String path, SubscribeListener listener) {
        registry.subscribe(path, listener);
    }

    public void addConnectionStateListener(ConnectionListener listener) {
        registry.addConnectionStateListener(listener);
    }

    public boolean exists(String key) {
        return registry.exists(key);
    }

    public boolean getLock(String key) {
        return registry.acquireLock(key);
    }

    public boolean releaseLock(String key) {
        return registry.releaseLock(key);
    }

    public void setStoppable(IStoppable stoppable) {
        this.stoppable = stoppable;
    }

    public IStoppable getStoppable() {
        return stoppable;
    }

    public boolean isMasterPath(String path) {
        return path != null && path.startsWith(Constants.REGISTRY_DTS_MASTERS);
    }

    public Collection<String> getChildrenKeys(final String key) {
        return registry.children(key);
    }

    public Set<String> getServerNodeSet(NodeType nodeType, boolean hostOnly) {
        try {
            return getServerNodes(nodeType).stream().map(server -> {
                return server;
            }).collect(Collectors.toSet());
        } catch (Exception e) {
            throw new RegistryException("Failed to get server node: " + nodeType, e);
        }
    }

    private void initNodes() {
        registry.put(Constants.REGISTRY_DTS_MASTERS, EMPTY, false);
    }

    private String rootNodePath(NodeType type) {
        switch (type) {
            case MASTER:
                return Constants.REGISTRY_DTS_MASTERS;
            default:
                throw new IllegalStateException("Should not reach here");
        }
    }

    private Collection<String> getServerNodes(NodeType nodeType) {
        final String path = rootNodePath(nodeType);
        final Collection<String> serverList = getChildrenKeys(path);
        if (nodeType == NodeType.MASTER || nodeType == NodeType.DEAD_SERVER) {
            return serverList;
        }
        return serverList.stream().flatMap(group -> getChildrenKeys(path + Constants.SINGLE_SLASH + group)
                .stream()
                .map(it -> group + Constants.SINGLE_SLASH + it)).collect(Collectors.toList());
    }

}
