package com.bilibili.cluster.scheduler.common.dto.caster;

import cn.hutool.core.annotation.Alias;
import lombok.Data;

import java.util.Map;

@Data
public class ResourceNodeInfo {

    private String name;

    private String address;

    @Alias(value = "unschedulable")
    private boolean unSchedulable;

    private String hostname;

    private Map<String, String> labels;

}
