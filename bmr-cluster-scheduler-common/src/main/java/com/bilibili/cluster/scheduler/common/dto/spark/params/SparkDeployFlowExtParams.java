package com.bilibili.cluster.scheduler.common.dto.spark.params;

import lombok.Data;

import java.util.List;

@Data
public class SparkDeployFlowExtParams {

    /**
     * spark 目标版本
     */
    private String targetSparkVersion;

    /**
     * 待回滚的版本
     */
    private String originalSparkVersion;

    /**
     * spark 大版本
     */
    private String majorSparkVersion;


    /**
     * 全量发布的阶段比例
     * 如: [1, 10, 20, 50, 100]
     */
    private List<Integer> percentStageList;

    /**
     * spark-发布场景
     */
    private SparkDeployType sparkDeployType;

    /**
     * 发布审批人
     */
    private List<String> approverList;

    /**
     * 抄送人
     */
    private List<String> ccList;

    /**
     * 审批title
     */
    private String title;

    /**
     * 发布说明
     */
    private String remark;

}
