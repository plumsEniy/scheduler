package com.bilibili.cluster.scheduler.common.dto.spark.client;


import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum SparkClientDeployType {

    ADD_NEWLY_HOSTS("新增节点"),

    ADD_NEWLY_VERSION("新增版本"),

    REMOVE_USELESS_HOSTS("删除主机"),

    REMOVE_USELESS_VERSION("删除版本"),
    ;

    String desc;

}
