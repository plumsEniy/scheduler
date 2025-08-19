package com.bilibili.cluster.scheduler.common.dto.caster;

import lombok.Data;

import java.util.Map;

/**
 * @description:
 * @Date: 2025/4/9 20:30
 * @Author: nizhiqiang
 */

@Data
public class PvcMetadata {

    private String name;

    private String namespace;

    private String uid;

    private String resourceVersion;

    private String creationTimestamp;

    private Map<String, String> labels;

}
