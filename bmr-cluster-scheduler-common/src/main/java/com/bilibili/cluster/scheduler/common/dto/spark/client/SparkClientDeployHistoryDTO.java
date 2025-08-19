package com.bilibili.cluster.scheduler.common.dto.spark.client;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SparkClientDeployHistoryDTO {

    private SparkClientType clientType;

    private String tagName;

    @JSONField(format = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime ctime;

    private String deployPlatform = "Spark Manager";

}
