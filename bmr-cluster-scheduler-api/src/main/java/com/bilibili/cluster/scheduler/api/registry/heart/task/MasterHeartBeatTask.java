package com.bilibili.cluster.scheduler.api.registry.heart.task;

import cn.hutool.json.JSONUtil;
import com.bilibili.cluster.scheduler.api.configuration.MasterConfig;
import com.bilibili.cluster.scheduler.api.dto.registry.BaseHeartBeatTask;
import com.bilibili.cluster.scheduler.api.dto.registry.MasterHeartBeat;
import com.bilibili.cluster.scheduler.api.registry.service.impl.RegistryClient;
import com.bilibili.cluster.scheduler.common.lifecycle.ServerLifeCycleManager;
import com.bilibili.cluster.scheduler.common.utils.OSUtils;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MasterHeartBeatTask extends BaseHeartBeatTask<MasterHeartBeat> {

    private final MasterConfig masterConfig;

    private final RegistryClient registryClient;

    private final String heartBeatPath;

    private final int processId;

    public MasterHeartBeatTask(@NonNull MasterConfig masterConfig,
                               @NonNull RegistryClient registryClient) {
        super("MasterHeartBeatTask", masterConfig.getHeartbeatInterval().toMillis());
        this.masterConfig = masterConfig;
        this.registryClient = registryClient;
        this.heartBeatPath = masterConfig.getMasterRegistryPath();
        this.processId = OSUtils.getProcessID();
    }

    @Override
    public MasterHeartBeat getHeartBeat() {
        return MasterHeartBeat.builder()
                .startupTime(ServerLifeCycleManager.getServerStartupTime())
                .reportTime(System.currentTimeMillis())
                .cpuUsage(OSUtils.cpuUsage())
                .loadAverage(OSUtils.loadAverage())
                .availablePhysicalMemorySize(OSUtils.availablePhysicalMemorySize())
                .memoryUsage(OSUtils.memoryUsage())
                .diskAvailable(OSUtils.diskAvailable())
                .processId(processId)
                .build();
    }

    @Override
    public void writeHeartBeat(MasterHeartBeat masterHeartBeat) {
        String masterHeartBeatJson = JSONUtil.toJsonStr(masterHeartBeat);
        registryClient.persistEphemeral(heartBeatPath, masterHeartBeatJson);
        log.debug("Success write master heartBeatInfo into registry, masterRegistryPath: {}, heartBeatInfo: {}",
                heartBeatPath, masterHeartBeatJson);
    }
}
