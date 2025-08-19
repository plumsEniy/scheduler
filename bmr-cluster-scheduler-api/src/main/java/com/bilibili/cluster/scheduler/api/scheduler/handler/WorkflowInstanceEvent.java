package com.bilibili.cluster.scheduler.api.scheduler.handler;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class WorkflowInstanceEvent {

    private WorkflowInstanceType workflowEventType;

    private Long instanceId;

    private Long flowId;

}
