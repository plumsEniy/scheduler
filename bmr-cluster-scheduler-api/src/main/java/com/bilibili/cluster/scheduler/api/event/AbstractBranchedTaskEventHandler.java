package com.bilibili.cluster.scheduler.api.event;

import com.bilibili.cluster.scheduler.common.entity.ExecutionNodeEntity;
import com.bilibili.cluster.scheduler.common.enums.node.NodeExecType;
import com.bilibili.cluster.scheduler.common.enums.node.NodeType;
import com.bilibili.cluster.scheduler.common.event.TaskEvent;


/**
 * 通用的普通节点、逻辑节点事件处理器
 * 支持设置是否回滚，默认不开启
 * 支持设置是否跳过普通节点、逻辑节点，默认分别不开启、开启
 * 若存在回滚场景，需要分别处理普通节点和逻辑节点的正常执行和回滚执行
 */
public abstract class AbstractBranchedTaskEventHandler extends AbstractTaskEventHandler {

    // 是否存在回滚分支
    protected boolean hasRollbackBranch() {
        return false;
    }

    // 是否跳过逻辑节点
    protected boolean skipLogicalNode() {
        return true;
    }

    // 是否跳过普通节点
    protected boolean skipNormalNode() {
        return false;
    }

    @Override
    public boolean executeTaskEvent(TaskEvent taskEvent) throws Exception {
        final ExecutionNodeEntity executionNode = taskEvent.getExecutionNode();
        final NodeType nodeType = executionNode.getNodeType();

        if (nodeType.isNormalNode()) {
            return handleNormalNodeTaskEvent(taskEvent);
        }
        return handleLogicalNodeTaskEvent(taskEvent);
    }

    /**
     * 处理普通节点
     *
     * @param taskEvent
     * @return
     * @throws Exception
     */
    protected boolean handleNormalNodeTaskEvent(TaskEvent taskEvent) throws Exception {
        final ExecutionNodeEntity executionNode = taskEvent.getExecutionNode();
        if (skipNormalNode()) {
            logPersist(taskEvent, "event type of '" + taskEvent.getEventEntity().getEventType()
                    + "' skip normal node is true, skipped.");
            return true;
        }
        logPersist(taskEvent, "event type of '" + taskEvent.getEventEntity().getEventType()
                + "' require process normal node.");
        final NodeExecType execType = executionNode.getExecType();
        if (hasRollbackBranch()) {
            logPersist(taskEvent, "event type of '" + taskEvent.getEventEntity().getEventType()
                    + "' has rollback branch, start process event");
            if (execType.isRollbackState()) {
                logPersist(taskEvent, "event type of '" + taskEvent.getEventEntity().getEventType()
                        + "' on rollback state");
                return executeNormalNodeOnRollbackStateTaskEvent(taskEvent);
            } else {
                logPersist(taskEvent, "event type of '" + taskEvent.getEventEntity().getEventType()
                        + "' on forward state");
                return executeNormalNodeOnForwardStateTaskEvent(taskEvent);
            }
        } else {
            logPersist(taskEvent, "event type of '" + taskEvent.getEventEntity().getEventType()
                    + "' only forward branch, start process event");
            return executeNormalNodeTaskEvent(taskEvent);
        }
    }

    /**
     * 处理普通节点，并处于正向处理的逻辑
     *
     * @param taskEvent
     * @return
     */
    protected boolean executeNormalNodeOnForwardStateTaskEvent(TaskEvent taskEvent) throws Exception {
        logPersist(taskEvent, "default of empty 'executeNormalNodeOnForwardStateTaskEvent' func process, skip...");
        return true;
    }

    /**
     * 处理普通节点，并处于回滚状态的逻辑
     *
     * @param taskEvent
     * @return
     */
    protected boolean executeNormalNodeOnRollbackStateTaskEvent(TaskEvent taskEvent) throws Exception {
        logPersist(taskEvent, "default of empty 'executeNormalNodeOnRollbackStateTaskEvent' func process, skip...");
        return true;
    }

    /**
     * 处理普通节点，不存在回滚分支的场景
     *
     * @param taskEvent
     * @return
     */
    protected boolean executeNormalNodeTaskEvent(TaskEvent taskEvent) throws Exception {
        logPersist(taskEvent, "default of empty 'executeNormalNodeTaskEvent' func process, skip...");
        return true;
    }


    /**
     * 处理逻辑节点
     *
     * @param taskEvent
     * @return
     * @throws Exception
     */
    protected boolean handleLogicalNodeTaskEvent(TaskEvent taskEvent) throws Exception {
        final ExecutionNodeEntity executionNode = taskEvent.getExecutionNode();
        final NodeExecType execType = executionNode.getExecType();
        if (skipLogicalNode()) {
            logPersist(taskEvent, "event type of '" + taskEvent.getEventEntity().getEventType()
                    + "' skip logical node is true, skipped.");
            return true;
        }
        logPersist(taskEvent, "event type of '" + taskEvent.getEventEntity().getEventType()
                + "' require process logical node.");
        if (hasRollbackBranch()) {
            logPersist(taskEvent, "event type of '" + taskEvent.getEventEntity().getEventType()
                    + "' has rollback branch, start process event");
            if (execType.isRollbackState()) {
                logPersist(taskEvent, "event type of '" + taskEvent.getEventEntity().getEventType()
                        + "' on rollback state");
                return executeLogicalNodeOnRollbackStateTaskEvent(taskEvent);
            } else {
                logPersist(taskEvent, "event type of '" + taskEvent.getEventEntity().getEventType()
                        + "' on forward state");
                return executeLogicalNodeOnForwardStateTaskEvent(taskEvent);
            }
        } else {
            logPersist(taskEvent, "event type of '" + taskEvent.getEventEntity().getEventType()
                    + "' only forward branch, start process event");
            return executeLogicalNodeTaskEvent(taskEvent);
        }
    }

    /**
     * 处理逻辑节点，不存在回滚分支的场景
     *
     * @param taskEvent
     * @return
     * @throws Exception
     */
    protected boolean executeLogicalNodeTaskEvent(TaskEvent taskEvent) throws Exception {
        logPersist(taskEvent, "default of empty 'executeLogicalNodeTaskEvent' func process, skip...");
        return true;
    }

    /**
     * 处理逻辑节点，并处于回滚状态的逻辑
     *
     * @param taskEvent
     * @return
     */
    protected boolean executeLogicalNodeOnRollbackStateTaskEvent(TaskEvent taskEvent) {
        logPersist(taskEvent, "default of empty 'executeLogicalNodeOnRollbackStateTaskEvent' func process, skip...");
        return true;
    }

    /**
     * 处理逻辑节点，并处于正向处理的逻辑
     *
     * @param taskEvent
     * @return
     */
    protected boolean executeLogicalNodeOnForwardStateTaskEvent(TaskEvent taskEvent) {
        logPersist(taskEvent, "default of empty 'executeLogicalNodeOnForwardStateTaskEvent' func process, skip...");
        return true;
    }

}
