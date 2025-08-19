package com.bilibili.cluster.scheduler.api.event.analyzer;

import com.bilibili.cluster.scheduler.common.enums.dolphin.TaskPosType;
import com.bilibili.cluster.scheduler.common.enums.scheduler.DolpFailureStrategy;
import lombok.Data;

@Data
public class ResolvedEvent extends UnResolveEvent {

    private String eventName;

    private String taskCode;

    private TaskPosType taskPosType;

}
