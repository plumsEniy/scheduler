package com.bilibili.cluster.scheduler.common.dto.scheduler.model;

import lombok.Data;

import java.util.List;

@Data
public class PipelineDefine {

    private String projectName;

    private String pipelineName;

    private String projectCode;

    private String pipelineCode;

    private List<SchedTaskDefine> schedTaskDefineList;
}
