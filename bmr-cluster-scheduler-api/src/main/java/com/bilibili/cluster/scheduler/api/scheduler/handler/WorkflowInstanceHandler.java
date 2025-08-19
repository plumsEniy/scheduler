package com.bilibili.cluster.scheduler.api.scheduler.handler;


import com.bilibili.cluster.scheduler.api.exceptions.WorkflowIntanceHandleError;
import com.bilibili.cluster.scheduler.api.exceptions.WorkflowIntanceHandleException;

public interface WorkflowInstanceHandler {

    /**
     * Handle a workflow instance,
     *
     * @throws WorkflowIntanceHandleError     if this exception happen, means the event is broken, need to drop this event.
     * @throws WorkflowIntanceHandleException if this exception happen, means we need to retry this event.
     */
    void handleWorkflowInstance(WorkflowInstanceEvent workflowInstanceEvent) throws WorkflowIntanceHandleError, WorkflowIntanceHandleException;

    WorkflowInstanceType getHandleWorkflowInstanceType();
}
