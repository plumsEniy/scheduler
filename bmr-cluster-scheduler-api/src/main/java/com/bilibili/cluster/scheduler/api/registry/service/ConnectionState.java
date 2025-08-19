package com.bilibili.cluster.scheduler.api.registry.service;

/**
 * Connection state between client and registry center(Etcd, MySql, Zookeeper)
 */
public enum ConnectionState {
    CONNECTED,
    RECONNECTED,
    SUSPENDED,
    DISCONNECTED
}
