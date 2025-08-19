package com.bilibili.cluster.scheduler.common.dto.flow;

import lombok.Data;

import java.util.List;

/**
 * @description: saber任务更新的属性
 * @Date: 2024/1/25 19:45
 * @Author: nizhiqiang
 */
@Data
public class SaberUpdateProp {

    /**
     * 集群id
     */
    private int cluster;

    /**
     * 任务队列
     */
    private String queue;

    /**
     * 运行时优先级
     */
    private int level;

    /**
     * 并发度
     */
    private int parallelism;

    /**
     * tm内存
     */
    private int taskManagerMemory;

    /**
     * flink大版本
     */
    private String flinkVersion;

    /**
     * ExcaliburSessionReuse
     */
    private boolean reuseSession;

    /**
     * 任务参数
     */
    private String mainArgs;

    /**
     * 集群参数
     */
    private String sysParameters;

    /**
     * udf jar路径
     */
    private List<String> udfJarPath;

    /**
     * sql字段
     */
    private String bsql;

    /**
     * flink小版本
     */
    private String leafTag;

    /**
     * task slot数量
     */
    private int numberOfTaskSlots;

    /**
     * 是否newsession
     */
    private boolean newSession;

    /**
     * 驱逐主机（任务重启后不会发布到该主机上）
     */
    private String blackHosts;

    /**
     * 是否为session job
     */
    private Boolean sessionJob;

    /**
     * 填充额外的tag属性信息
     */
    private List<String> tagList;

}
