
package com.bilibili.cluster.scheduler.api.registry.service;

/**
 * when the connect state between client and registry center changed,
 * the {@code onUpdate} function is triggered
 */
@FunctionalInterface
public interface ConnectionListener {

    void onUpdate(ConnectionState newState);
}
