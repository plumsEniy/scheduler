package com.bilibili.cluster.scheduler.common.enums.bmr.metadata;

import com.fasterxml.jackson.annotation.JsonFormat;

@JsonFormat(shape = JsonFormat.Shape.OBJECT)

public enum ClusterTypeEnum {

    CLOUD_HOST_CLUSTER(" CLOUD_HOST_CLUSTER ","云主机集群"),
    PHYSICAL_MACHINE_CLUSTER("PHYSICAL_MACHINE_CLUSTER","物理机集群"),
    CONTAINER_CLUSTER("CONTAINER_CLUSTER","容器集群"),
    VIRTUAL_MACHINE_CLUSTER("VIRTUAL_MACHINE_CLUSTER","虚拟机集群");

    private String value;
    private String key;

    ClusterTypeEnum(String key, String value) {
        this.key = key;
        this.value = value;
    }

    public String getValue() {
        return value;
    }
    public String getKey() {
        return key;
    }
}
