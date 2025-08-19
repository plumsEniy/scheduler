package com.bilibili.cluster.scheduler.common.dto.presto.scaler;

import com.bilibili.cluster.scheduler.common.dto.tide.type.DynamicScalingStrategy;
import lombok.Data;

@Data
public class PrestoFastScalerExtFlowParams {

    // 预期扩容的节点数量
    private int highPodNum;

    // 预期缩容的节点数量
    private int lowPodNum;

    // 校验阈值
    private double threshold = 0.85d;

    // 扩缩策略
    private DynamicScalingStrategy dynamicScalingStrategy;

}
