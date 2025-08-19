package com.bilibili.cluster.scheduler.api.scheduler.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.LinkedBlockingQueue;

@Component
public class WorkflowInstanceQueue {

    private final Logger logger = LoggerFactory.getLogger(WorkflowInstanceQueue.class);

    private static final LinkedBlockingQueue<WorkflowInstanceEvent> workflowEventQueue = new LinkedBlockingQueue<>();

    /**
     * Add a workflow event.
     */
    public void addEvent(WorkflowInstanceEvent workflowInstance) {
        workflowEventQueue.add(workflowInstance);
        logger.info("Added workflow instance to WorkflowInstance queue, event: {}", workflowInstance);
    }

    /**
     * Pool the head of the workflow event queue and wait an workflow event.
     */
    public WorkflowInstanceEvent poolEvent() throws InterruptedException {
        return workflowEventQueue.take();
    }

    public void clearWorkflowEventQueue() {
        workflowEventQueue.clear();
    }
}
