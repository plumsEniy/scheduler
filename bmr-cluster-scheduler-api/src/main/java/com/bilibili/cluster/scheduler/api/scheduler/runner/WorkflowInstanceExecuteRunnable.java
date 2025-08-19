
package com.bilibili.cluster.scheduler.api.scheduler.runner;


import com.bilibili.cluster.scheduler.api.exceptions.WorkflowInstanceTaskEventHandleException;
import com.bilibili.cluster.scheduler.api.scheduler.handler.WorkflowInstanceTaskEvent;
import com.bilibili.cluster.scheduler.api.scheduler.handler.WorkflowStartInstanceTaskEventHandler;
import com.bilibili.cluster.scheduler.api.service.flow.ExecutionNodeService;
import com.bilibili.cluster.scheduler.common.Constants;
import com.bilibili.cluster.scheduler.common.dto.flow.ExecutionFlowInstanceDTO;
import com.bilibili.cluster.scheduler.common.enums.flow.FlowStatusEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.concurrent.Callable;

/**
 * Workflow execute task, used to execute a workflow instance.
 */
public class WorkflowInstanceExecuteRunnable implements Callable<WorkflowIntanceSubmitStatue> {

    private static final Logger logger = LoggerFactory.getLogger(WorkflowInstanceExecuteRunnable.class);

    private WorkflowRunnableStatus workflowRunnableStatus = WorkflowRunnableStatus.CREATED;

    private final String masterAddress;

    private ExecutionFlowInstanceDTO executionFlowInstanceDTO;

    private WorkflowStartInstanceTaskEventHandler workflowStartInstanceTaskEventHandler;

    private ExecutionNodeService executionNodeService;

    public WorkflowInstanceExecuteRunnable(ExecutionFlowInstanceDTO executionFlowInstanceDTO,
                                           WorkflowStartInstanceTaskEventHandler workflowStartInstanceTaskEventHandler,
                                           ExecutionNodeService executionNodeService,
                                           String masterAddress) {
        this.executionFlowInstanceDTO = executionFlowInstanceDTO;
        this.executionNodeService = executionNodeService;
        this.masterAddress = masterAddress;
        this.workflowStartInstanceTaskEventHandler = workflowStartInstanceTaskEventHandler;

    }

    /**
     * the process start nodes are submitted completely.
     */
    public boolean isStart() {
        return WorkflowRunnableStatus.STARTED == workflowRunnableStatus;
    }

    @Override
    public WorkflowIntanceSubmitStatue call() {
        if (isStart()) {
            // This case should not been happened
            logger.warn("[WorkflowInstance-{}] Workflow The workflow has already been started", executionFlowInstanceDTO.getInstanceId());
            return WorkflowIntanceSubmitStatue.DUPLICATED_SUBMITTED;
        }

        try {
            Long instanceId = executionFlowInstanceDTO.getInstanceId();
            Long flowId = executionFlowInstanceDTO.getFlowId();
            FlowStatusEnum flowStatusEnum = executionFlowInstanceDTO.getFlowStatusEnum();
            Integer currentBatchId = executionFlowInstanceDTO.getCurrentBatchId();
            MDC.put(Constants.WORK_FLOW_PROCESS_INSTANCE_ID_MDC_KEY, String.valueOf(instanceId));

            // 生成事件集合
            if (workflowRunnableStatus == WorkflowRunnableStatus.CREATED) {
                workflowRunnableStatus = WorkflowRunnableStatus.EXECUTE_EVENT;
                logger.warn("Workflow workflowInstanceStatue changed to :{}, instanceId:{}", workflowRunnableStatus, instanceId);
            }


            // 执行事件集合
            if (workflowRunnableStatus == WorkflowRunnableStatus.EXECUTE_EVENT) {

                WorkflowInstanceTaskEvent workflowInstanceTaskEvent = new WorkflowInstanceTaskEvent(
                        instanceId,
                        currentBatchId,
                        flowId,
                        masterAddress,
                        executionFlowInstanceDTO);


                executeWorkProcessTaskEvent(workflowInstanceTaskEvent);
                workflowRunnableStatus = WorkflowRunnableStatus.STARTED;
                logger.warn("Workflow workflowInstanceStatue changed to :{}, instanceId:{}", workflowRunnableStatus, instanceId);

            }

            return WorkflowIntanceSubmitStatue.SUCCESS;
        } catch (Exception e) {
            logger.error("Workflow Start workflowInstance error", e);
            // throw new WorkflowIntanceHandleException("Workflow Start workflowInstance error", e);
        } finally {
            MDC.remove(Constants.WORK_FLOW_PROCESS_INSTANCE_ID_MDC_KEY);
        }
        return WorkflowIntanceSubmitStatue.FAILED;
    }


    private void executeWorkProcessTaskEvent(WorkflowInstanceTaskEvent workflowInstanceTaskEvent) throws WorkflowInstanceTaskEventHandleException {
        workflowStartInstanceTaskEventHandler.handleWorkflowInstanceTaskEvent(workflowInstanceTaskEvent);
    }

    public WorkflowRunnableStatus getWorkflowRunnableStatus() {
        return workflowRunnableStatus;
    }

    public String getMasterAddress() {
        return masterAddress;
    }

    private enum WorkflowRunnableStatus {
        CREATED, INITIALIZE_EVENT, EXECUTE_EVENT, STARTED;
    }

}
