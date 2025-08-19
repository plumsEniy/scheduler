package com.bilibili.cluster.scheduler.common.dto.spark.periphery;

import com.bilibili.cluster.scheduler.common.dto.spark.params.SparkDeployType;
import lombok.Data;

import java.util.List;

/**
 * spark周边组件发布通用参数
 */
@Data
public class SparkPeripheryComponentDeployFlowExtParams {

    /**
     * 周边组件类型
     */
    private SparkPeripheryComponent peripheryComponent;

    /**
     * 普通发布 or 紧急发布
     */
    private SparkDeployType sparkDeployType;

    /**
     * 是否跳过阶段暂停，true为跳过
     */
    private boolean skipStagePause = false;

    /**
     * 目标版本
     */
    private String targetVersion;

    /**
     * 初始版本（仅在全量发布回滚场景使用）
     */
    private String originalVersion;

    /**
     * 全量发布的阶段比例
     * 如: [1, 10, 20, 50, 100]
     */
    private List<Integer> percentStageList;

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
