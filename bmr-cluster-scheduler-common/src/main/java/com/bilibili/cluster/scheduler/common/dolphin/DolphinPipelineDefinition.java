package com.bilibili.cluster.scheduler.common.dolphin;

import com.bilibili.cluster.scheduler.common.dto.scheduler.model.SchedTaskDefine;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class DolphinPipelineDefinition {

    private String projectCode;

    private String pipelineCode;

    // 流程命名索引
    private int schedulerPipelineChainIndex;

    // task struct
    private List<SchedTaskDefine> schedTaskDefineList;

}
