package com.bilibili.cluster.scheduler.common.dto.yarn;

import lombok.Data;

@Data
public class InitResource {
    private int memory = 0;
    private int vCores = 0;
}
