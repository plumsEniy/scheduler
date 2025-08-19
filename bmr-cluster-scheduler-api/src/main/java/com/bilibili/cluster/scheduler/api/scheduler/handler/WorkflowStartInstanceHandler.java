
package com.bilibili.cluster.scheduler.api.scheduler.handler;

import com.bilibili.cluster.scheduler.api.exceptions.WorkflowIntanceHandleError;
import com.bilibili.cluster.scheduler.api.scheduler.runner.WorkflowIntanceSubmitStatue;
import com.bilibili.cluster.scheduler.api.scheduler.cache.ProcessInstanceExecCacheManager;
import com.bilibili.cluster.scheduler.api.scheduler.runner.WorkflowInstanceExecuteRunnable;
import com.bilibili.cluster.scheduler.api.scheduler.runner.WorkflowInstanceExecuteThreadPool;
import com.bilibili.cluster.scheduler.common.utils.ThreadUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
public class WorkflowStartInstanceHandler implements WorkflowInstanceHandler {

    @Resource
    private ProcessInstanceExecCacheManager processInstanceExecCacheManager;

    @Resource
    private WorkflowInstanceExecuteThreadPool workflowInstanceExecuteThreadPool;

    @Resource
    WorkflowInstanceQueue workflowEventQueue;

    @Override
    public void handleWorkflowInstance(final WorkflowInstanceEvent workflowInstanceEvent) throws WorkflowIntanceHandleError {
        log.info("Workflow Handle workflowInstance start event, begin to start a workflow, instance: {}", workflowInstanceEvent);
        WorkflowInstanceExecuteRunnable workflowInstanceExecuteRunnable = processInstanceExecCacheManager.getByProcessInstanceId(workflowInstanceEvent.getInstanceId());
        if (workflowInstanceExecuteRunnable == null) {
            log.error("Workflow The workflow instance start is invalid, cannot find the workflow instance from cache");
            throw new WorkflowIntanceHandleError(
                    "Workflow The workflow instance start is invalid, cannot find the workflow instance from cache");
        }

        CompletableFuture.supplyAsync(workflowInstanceExecuteRunnable::call, workflowInstanceExecuteThreadPool)
            .thenAccept(workflowSubmitStatue -> {
                if (WorkflowIntanceSubmitStatue.SUCCESS == workflowSubmitStatue) {
                    log.info("Workflow Success submit the workflow instance");
                    processInstanceExecCacheManager.removeByProcessInstanceId(workflowInstanceEvent.getInstanceId());
                } else {
                    log.error("Workflow Failed to submit the workflow instance, will resend the workflow start instance: {}", workflowInstanceEvent);
//                    ThreadUtils.sleep(10_000);
//                    workflowEventQueue.addEvent(workflowInstanceEvent);
                }
            });
    }

    @Override
    public WorkflowInstanceType getHandleWorkflowInstanceType() {
        return WorkflowInstanceType.START_WORKFLOW;
    }
}
