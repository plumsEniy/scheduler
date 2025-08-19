package com.bilibili.cluster.scheduler.common.dto.button;

import lombok.Data;

@Data
public class DeployStageInfo {

    private int stage;

    private StageStateEnum state;

    private String startTime;

    private String endTime;

//    private int nodeSize;

    private String allowedNextStageStartTime;


    public DeployStageInfo(int stage) {
        this.stage = stage;
    }

}
