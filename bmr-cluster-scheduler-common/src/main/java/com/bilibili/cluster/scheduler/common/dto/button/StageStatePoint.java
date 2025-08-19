package com.bilibili.cluster.scheduler.common.dto.button;

import lombok.Data;

@Data
public class StageStatePoint {

    private int id;

    private String content;

    private StageStateEnum status;

    private String startTime;

    private String endTime;

    private String allowedNextStageStartTime;
}
