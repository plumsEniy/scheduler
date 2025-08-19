
package com.bilibili.cluster.scheduler.api.dto.registry;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MasterHeartBeat {

    private long startupTime;
    private long reportTime;
    private double cpuUsage;
    private double memoryUsage;
    private double loadAverage;
    private double availablePhysicalMemorySize;
    private double maxCpuloadAvg;
    private double reservedMemory;
    private double diskAvailable;
    private int processId;
}
