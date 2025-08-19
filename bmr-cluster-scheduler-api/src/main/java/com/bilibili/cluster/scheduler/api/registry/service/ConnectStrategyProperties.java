
package com.bilibili.cluster.scheduler.api.registry.service;

import com.bilibili.cluster.scheduler.api.dto.registry.StrategyType;
import lombok.Data;

import java.time.Duration;

@Data
public class ConnectStrategyProperties {

    private StrategyType strategy = StrategyType.STOP;

    private Duration maxWaitingTime = Duration.ofSeconds(0);

}
