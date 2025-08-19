package com.bilibili.cluster.scheduler.api.scheduler.handler;

import com.bilibili.cluster.scheduler.api.exceptions.WorkflowIntanceHandleError;
import com.bilibili.cluster.scheduler.api.exceptions.WorkflowInstanceTaskEventHandleException;
import com.bilibili.cluster.scheduler.common.entity.ExecutionNodeEntity;

import java.util.List;

public interface WorkflowInstanceTaskEventHandler {


    /**
     * Handle a workflow instance task event,
     *
     * @throws WorkflowIntanceHandleError if this exception happen, means the event is broken, need to drop this event.
     */
    void handleWorkflowInstanceTaskEvent(WorkflowInstanceTaskEvent workflowInstanceTaskEvent) throws WorkflowInstanceTaskEventHandleException;


    void handleWorkflowJobTaskEvent(WorkflowInstanceTaskEvent workflowInstanceTaskEvent, List<ExecutionNodeEntity> executionJobEntities) throws WorkflowInstanceTaskEventHandleException;


}
