package com.bilibili.cluster.scheduler.api.scheduler.handler;

import cn.hutool.json.JSONUtil;
import com.bilibili.cluster.scheduler.api.exceptions.WorkflowInstanceTaskEventHandleException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class AbstractWorkflowInstanceTaskEventHandler implements WorkflowInstanceTaskEventHandler {

    protected void preHandleWorkflowInstanceTaskEvent(WorkflowInstanceTaskEvent workflowInstanceTaskEvent) throws WorkflowInstanceTaskEventHandleException {
        log.info("preHandleWorkflowInstanceTaskEvent:{}", JSONUtil.toJsonStr(workflowInstanceTaskEvent));
    }

    protected void afterHandleWorkflowInstanceTaskEvent(WorkflowInstanceTaskEvent workflowInstanceTaskEvent) throws WorkflowInstanceTaskEventHandleException {
        log.info("afterHandleWorkflowInstanceTaskEvent:{}", JSONUtil.toJsonStr(workflowInstanceTaskEvent));
    }


    @Override
    public void handleWorkflowInstanceTaskEvent(WorkflowInstanceTaskEvent workflowInstanceTaskEvent) throws WorkflowInstanceTaskEventHandleException {
        try {
            preHandleWorkflowInstanceTaskEvent(workflowInstanceTaskEvent);
            executeWorkflowInstanceTaskEvent(workflowInstanceTaskEvent);
            afterHandleWorkflowInstanceTaskEvent(workflowInstanceTaskEvent);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new WorkflowInstanceTaskEventHandleException(e.getMessage(), e);
        }

    }

    public abstract void executeWorkflowInstanceTaskEvent(WorkflowInstanceTaskEvent workflowInstanceTaskEvent) throws WorkflowInstanceTaskEventHandleException;


}
