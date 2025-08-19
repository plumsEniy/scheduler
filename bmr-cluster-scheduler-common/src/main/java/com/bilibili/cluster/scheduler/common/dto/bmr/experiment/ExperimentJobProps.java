package com.bilibili.cluster.scheduler.common.dto.bmr.experiment;

import com.bilibili.cluster.scheduler.common.dto.params.BaseNodeParams;
import lombok.Data;

@Data
public class ExperimentJobProps extends BaseNodeParams {

    private String opUser;

    private String jobId;

    private String jobName;

    private ExperimentJobType jobType;

    private ExperimentType experimentType;

    private String platformA;

    private String platformB;

    private String confA;

    private String confB;

    private String metrics;

    private long testSetVersionId;

    private long ciInstanceId;

    // 实验id
    private String experimentId;
    // 实验开始时间
    private long startTs;

    private String sqlCode;


}
