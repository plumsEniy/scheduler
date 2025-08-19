package com.bilibili.cluster.scheduler.common.dto.spark.plus;

import lombok.Data;

@Data
public class SparkManagerJob {

    private String jobId;

    private String businessTime;

    private String codeA;

    private String targetTableA;


    // -----------选填参数-----------------

    private String codeB;

    private String targetTableB;

    /**
     * job级别的配置
     * map<String, Object>, selectColumn,List<String>
     */
    private String conf;

}
