package com.bilibili.cluster.scheduler.common.dto.flow;


import com.alibaba.fastjson.annotation.JSONField;
import com.bilibili.cluster.scheduler.common.enums.flow.FlowDeployType;
import com.bilibili.cluster.scheduler.common.enums.flow.FlowStatusEnum;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SparkDeployFlowInfoDTO<T> {

    private FlowDeployType deployType;

    private FlowStatusEnum flowStatus;

    /**
     * spark 大版本
     */
    private String majorSparkVersion;

    /**
     * spark 目标版本
     */
    private String targetSparkVersion;

    private String host;

    private String opUser;

    @JSONField(format = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    @JSONField(format = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;

    @JSONField(format = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime ctime;

    @JSONField(format = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime mtime;

    private Long flowId;

    private String orderId;

    private String orderNo;

    private T extParams;

}
