package com.bilibili.cluster.scheduler.api.registry.service;


import com.bilibili.cluster.scheduler.api.dto.registry.RegistryEvent;

public interface SubscribeListener {

    void notify(RegistryEvent event);
}
