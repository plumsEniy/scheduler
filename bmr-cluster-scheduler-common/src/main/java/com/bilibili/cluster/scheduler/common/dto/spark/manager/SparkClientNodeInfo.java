package com.bilibili.cluster.scheduler.common.dto.spark.manager;

import lombok.Data;

@Data
public class SparkClientNodeInfo {

    private String creator;

    private String hostname;

    private String ip;

    private String oneClientPackageDirectory;

    private String sparkPackageDirectory;

    private String source;

}
