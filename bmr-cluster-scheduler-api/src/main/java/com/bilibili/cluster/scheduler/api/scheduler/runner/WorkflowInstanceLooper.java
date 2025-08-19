package com.bilibili.cluster.scheduler.api.scheduler.runner;

import com.bilibili.cluster.scheduler.api.exceptions.WorkflowIntanceHandleError;
import com.bilibili.cluster.scheduler.api.exceptions.WorkflowIntanceHandleException;
import com.bilibili.cluster.scheduler.api.scheduler.handler.WorkflowInstanceEvent;
import com.bilibili.cluster.scheduler.api.scheduler.handler.WorkflowInstanceHandler;
import com.bilibili.cluster.scheduler.api.scheduler.handler.WorkflowInstanceQueue;
import com.bilibili.cluster.scheduler.api.scheduler.handler.WorkflowInstanceType;
import com.bilibili.cluster.scheduler.common.Constants;
import com.bilibili.cluster.scheduler.common.lifecycle.ServerLifeCycleManager;
import com.bilibili.cluster.scheduler.common.thread.BaseDaemonThread;
import com.bilibili.cluster.scheduler.common.utils.ThreadUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class WorkflowInstanceLooper extends BaseDaemonThread {

    private final Logger logger = LoggerFactory.getLogger(WorkflowInstanceLooper.class);

    @Resource
    private WorkflowInstanceQueue workflowInstanceQueue;

    @Resource
    private List<WorkflowInstanceHandler> workflowInstanceHandlerList;

    private final Map<WorkflowInstanceType, WorkflowInstanceHandler> workflowInstanceHandlerMap = new HashMap<>();

    protected WorkflowInstanceLooper() {
        super("WorkflowEventLooper");
    }

    @PostConstruct
    public void init() {
        workflowInstanceHandlerList.forEach(
                workflowInstanceHandler -> workflowInstanceHandlerMap.put(workflowInstanceHandler.getHandleWorkflowInstanceType(),
                        workflowInstanceHandler));
    }

    @Override
    public synchronized void start() {
        logger.info("WorkflowEventLooper thread starting");
        super.start();
        logger.info("WorkflowEventLooper thread started");
    }

    public void run() {
        WorkflowInstanceEvent workflowIntance = null;
        while (!ServerLifeCycleManager.isStopped()) {
            try {
                workflowIntance = workflowInstanceQueue.poolEvent();
                MDC.put(Constants.WORK_FLOW_PROCESS_INSTANCE_ID_MDC_KEY, String.valueOf(workflowIntance.getInstanceId()));
                logger.info("Workflow event looper receive a workflow instance: {}, will handle this", workflowIntance);
                WorkflowInstanceHandler workflowInstanceHandler =
                        workflowInstanceHandlerMap.get(workflowIntance.getWorkflowEventType());
                workflowInstanceHandler.handleWorkflowInstance(workflowIntance);
            } catch (InterruptedException e) {
                logger.warn("Workflow WorkflowEventLooper thread is interrupted, will close this loop", e);
                Thread.currentThread().interrupt();
                break;
            } catch (WorkflowIntanceHandleException workflowEventHandleException) {
                logger.error("Workflow Handle workflow instance failed, will add this event to intance queue again, intance: {}",
                        workflowIntance, workflowEventHandleException);
                workflowInstanceQueue.addEvent(workflowIntance);
                ThreadUtils.sleep(Constants.SLEEP_TIME_MILLIS);
            } catch (WorkflowIntanceHandleError workflowEventHandleError) {
                logger.error("Workflow Handle workflow event error, will drop this event, event: {}",
                        workflowIntance,
                        workflowEventHandleError);
            } catch (Exception unknownException) {
                logger.error(
                        "Workflow Handle workflow instance failed, get a unknown exception, will add this event to intance queue again, intance: {}",
                        workflowIntance, unknownException);
                workflowInstanceQueue.addEvent(workflowIntance);
                ThreadUtils.sleep(Constants.SLEEP_TIME_MILLIS);
            } finally {
                MDC.remove(Constants.WORK_FLOW_PROCESS_INSTANCE_ID_MDC_KEY);
            }
        }
    }

}
