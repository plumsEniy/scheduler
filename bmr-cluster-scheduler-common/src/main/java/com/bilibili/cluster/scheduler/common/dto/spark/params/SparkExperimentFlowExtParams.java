package com.bilibili.cluster.scheduler.common.dto.spark.params;

import com.bilibili.cluster.scheduler.common.dto.bmr.experiment.ExperimentJobType;
import com.bilibili.cluster.scheduler.common.dto.bmr.experiment.ExperimentType;
import lombok.Data;


@Data
public class SparkExperimentFlowExtParams {

    private String platformA;

    private String platformB;

    private String imageA;

    private String imageB;

    /**
     * platformA 额外属性信息：如集群、队列信息
     * json数据
     */
    private String confA;

    /**
     * platformB 额外属性信息：如集群、队列信息
     * json数据
     */
    private String confB;

    private String metrics;

    /**
     * 实验任务类型
     */
    private ExperimentJobType jobType;

    /**
     * 实验类型: 对比实验 & 性能测试
     */
    private ExperimentType experimentType;

    /**
     * 测试集版本id
     */
    private long testSetVersionId;

    /**
     * spark manager 实验实例id
     */
    private long instanceId;


    /**
     * 实验一运行时配置
     */
    private String aRunTimeConf;

    /**
     * 实验二运行时配置
     */
    private String bRunTimeConf;


}
