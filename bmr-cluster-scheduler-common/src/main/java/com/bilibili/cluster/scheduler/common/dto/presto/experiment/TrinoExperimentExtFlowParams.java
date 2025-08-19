package com.bilibili.cluster.scheduler.common.dto.presto.experiment;


import com.bilibili.cluster.scheduler.common.dto.bmr.experiment.ExperimentType;
import lombok.Data;

@Data
public class TrinoExperimentExtFlowParams {

    // 实验类型
    private ExperimentType experimentType;

    // presto_a
    private String platformA;

    // presto_b
    private String platformB;

    // 实验集群A版本需要升级的镜像
    private String imageA;

    // 实验集群B版本需要升级的镜像
    private String imageB;

    /**
     * platformA 额外属性信息：
     *   {\"trino.client.tag\":\"ab_test_1\"}
     * json数据
     */
    private String confA;

    /**
     * platformA 额外属性信息：
     *     {\"trino.client.tag\":\"ab_test_2\"}
     * json数据
     */
    private String confB;

    /**
     * 测试集版本id
     */
    private long testSetVersionId;

    /**
     * spark manager 实验实例id
     */
    private long instanceId;

    /**
     * 集群A运行时配置信息: json数据
     * {@link TrinoClusterInfo}
     */
    private String aRunTimeConf;

    /**
     * 集群B运行时配置信息: json数据
     * {@link TrinoClusterInfo}
     */
    private String bRunTimeConf;

}
