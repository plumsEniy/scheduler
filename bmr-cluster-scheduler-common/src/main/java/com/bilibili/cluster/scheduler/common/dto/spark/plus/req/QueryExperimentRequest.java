package com.bilibili.cluster.scheduler.common.dto.spark.plus.req;

import lombok.Data;

@Data
public class QueryExperimentRequest {

    private String jobId;

    private String experimentId;

    private String platformSource;

    private String platformTarget;

}
