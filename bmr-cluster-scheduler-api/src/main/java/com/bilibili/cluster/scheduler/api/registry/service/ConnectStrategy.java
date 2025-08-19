
package com.bilibili.cluster.scheduler.api.registry.service;


import com.bilibili.cluster.scheduler.api.dto.registry.StrategyType;

/**
 * This interface defined a method to be executed when the server disconnected from registry.
 */
public interface ConnectStrategy {

    void disconnect();

    void reconnect();

    StrategyType getStrategyType();

}
