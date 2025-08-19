package com.bilibili.cluster.scheduler.common.enums;

import com.bilibili.cluster.scheduler.common.enums.event.EventStatusEnum;
import com.google.common.base.Preconditions;

/**
 * 任务节点状态
 */
public enum NodeExecuteStatusEnum {

    UN_NODE_EXECUTE("待变更"),
    IN_NODE_EXECUTE("变更中"),
    SUCCEED_NODE_EXECUTE("变更成功"),
    FAIL_NODE_EXECUTE("变更失败"),

    FAIL_SKIP_NODE_EXECUTE("变更失败-跳过"),

    UN_NODE_RETRY_EXECUTE("待重试"),
    IN_NODE_RETRY_EXECUTE("重试中"),
    SUCCEED_NODE_RETRY_EXECUTE("重试成功"),
    FAIL_NODE_RETRY_EXECUTE("重试失败"),

    FAIL_SKIP_NODE_RETRY_EXECUTE("重试失败-跳过"),

    SKIPPED("跳过"),
    UN_NODE_ROLLBACK_EXECUTE("待回滚"),
    IN_NODE_ROLLBACK_EXECUTE("回滚中"),
    SUCCEED_NODE_ROLLBACK_EXECUTE("回滚成功"),
    FAIL_NODE_ROLLBACK_EXECUTE("回滚失败"),
    FAIL_SKIP_NODE_ROLLBACK_EXECUTE("回滚失败-跳过"),

    RECOVERY_UN_NODE_EXECUTE("恢复-待变更"),
    RECOVERY_IN_NODE_EXECUTE("恢复-变更中"),
    RECOVERY_UN_NODE_RETRY_EXECUTE("恢复-待重试"),
    RECOVERY_IN_NODE_RETRY_EXECUTE("恢复-重试中"),
    RECOVERY_UN_NODE_ROLLBACK_EXECUTE("恢复-待回滚"),
    RECOVERY_IN_NODE_ROLLBACK_EXECUTE("恢复-回滚中"),

    ROLLBACK_SKIPPED("回滚-跳过"),
    ROLLBACK_SKIPPED_WHEN_UN_NODE_EXECUTE("回滚跳过(未执行)"),

    ;

    private String desc;

    NodeExecuteStatusEnum(String desc) {
        this.desc = desc;
    }

    public String getDesc() {
        return desc;
    }

    public boolean failRollBack() {
        switch (this) {
            case FAIL_NODE_ROLLBACK_EXECUTE:
            case FAIL_SKIP_NODE_ROLLBACK_EXECUTE:
                return true;
            default:
                return false;
        }
    }

    public boolean failExecute() {
        switch (this) {
            case FAIL_NODE_EXECUTE:
            case FAIL_SKIP_NODE_EXECUTE:
            case FAIL_NODE_RETRY_EXECUTE:
            case FAIL_SKIP_NODE_RETRY_EXECUTE:
                return true;
            default:
                return false;
        }
    }

    public boolean isFail() {
        return this.failExecute() || this.failRollBack();
    }

    public boolean isSuccessExecute() {
        switch (this) {
            case SUCCEED_NODE_EXECUTE:
            case SKIPPED:
            case SUCCEED_NODE_RETRY_EXECUTE:
                return true;
            default:
                return false;
        }
    }

    public boolean canRollBack() {
        switch (this) {
            case SUCCEED_NODE_EXECUTE:
            case FAIL_NODE_EXECUTE:
            case FAIL_SKIP_NODE_EXECUTE:
            case SUCCEED_NODE_RETRY_EXECUTE:
            case FAIL_NODE_RETRY_EXECUTE:
            case FAIL_SKIP_NODE_RETRY_EXECUTE:
                return true;
            default:
                return false;
        }
    }

    public boolean isInRollback() {
        switch (this) {
            case UN_NODE_ROLLBACK_EXECUTE:
            case IN_NODE_ROLLBACK_EXECUTE:
            case RECOVERY_UN_NODE_ROLLBACK_EXECUTE:
            case RECOVERY_IN_NODE_ROLLBACK_EXECUTE:
                return true;
            default:
                return false;
        }
    }

    public boolean isInExecute() {
        switch (this) {
            case IN_NODE_EXECUTE:
            case IN_NODE_RETRY_EXECUTE:
            case IN_NODE_ROLLBACK_EXECUTE:
            case RECOVERY_IN_NODE_EXECUTE:
            case RECOVERY_IN_NODE_RETRY_EXECUTE:
                return true;
            default:
                return false;
        }
    }

    public boolean isInRetry() {
        switch (this) {
            case UN_NODE_RETRY_EXECUTE:
            case IN_NODE_RETRY_EXECUTE:
            case RECOVERY_UN_NODE_RETRY_EXECUTE:
            case RECOVERY_IN_NODE_RETRY_EXECUTE:
                return true;
            default:
                return false;
        }
    }

    public boolean canSkip() {
        if (isInExecute()) {
            return true;
        }
        switch (this) {
            case UN_NODE_EXECUTE:
                return true;
            default:
                return false;
        }
    }

    public boolean isFinish() {
        switch (this) {
            case SKIPPED:
            case FAIL_NODE_EXECUTE:
            case SUCCEED_NODE_EXECUTE:
            case FAIL_NODE_RETRY_EXECUTE:
            case SUCCEED_NODE_RETRY_EXECUTE:
            case FAIL_SKIP_NODE_EXECUTE:
                return true;
            default:
                return false;
        }
    }

    public static NodeExecuteStatusEnum getNextExecStatus(NodeExecuteStatusEnum nodeStatus) {
        Preconditions.checkNotNull(nodeStatus, "node status is null");
        switch (nodeStatus) {
            case UN_NODE_EXECUTE:
                return IN_NODE_EXECUTE;
            case UN_NODE_RETRY_EXECUTE:
                return IN_NODE_RETRY_EXECUTE;
            case UN_NODE_ROLLBACK_EXECUTE:
                return IN_NODE_ROLLBACK_EXECUTE;
            case RECOVERY_UN_NODE_EXECUTE:
                return RECOVERY_IN_NODE_EXECUTE;
            case RECOVERY_UN_NODE_RETRY_EXECUTE:
                return RECOVERY_IN_NODE_RETRY_EXECUTE;
            case RECOVERY_UN_NODE_ROLLBACK_EXECUTE:
                return RECOVERY_IN_NODE_ROLLBACK_EXECUTE;
            default:
                return nodeStatus;
        }
    }

    public static NodeExecuteStatusEnum getRecoveryJobStatus(NodeExecuteStatusEnum nodeExecuteStatusEnum) throws Exception {
        switch (nodeExecuteStatusEnum) {
            case UN_NODE_EXECUTE:
            case RECOVERY_UN_NODE_EXECUTE:
                return NodeExecuteStatusEnum.RECOVERY_UN_NODE_EXECUTE;
            case IN_NODE_EXECUTE:
            case RECOVERY_IN_NODE_EXECUTE:
                return NodeExecuteStatusEnum.RECOVERY_IN_NODE_EXECUTE;
            case UN_NODE_RETRY_EXECUTE:
            case RECOVERY_UN_NODE_RETRY_EXECUTE:
                return NodeExecuteStatusEnum.RECOVERY_UN_NODE_RETRY_EXECUTE;
            case IN_NODE_RETRY_EXECUTE:
            case RECOVERY_IN_NODE_RETRY_EXECUTE:
                return NodeExecuteStatusEnum.RECOVERY_IN_NODE_RETRY_EXECUTE;
            case UN_NODE_ROLLBACK_EXECUTE:
            case RECOVERY_UN_NODE_ROLLBACK_EXECUTE:
                return NodeExecuteStatusEnum.RECOVERY_UN_NODE_ROLLBACK_EXECUTE;
            case IN_NODE_ROLLBACK_EXECUTE:
            case RECOVERY_IN_NODE_ROLLBACK_EXECUTE:
                return NodeExecuteStatusEnum.RECOVERY_IN_NODE_ROLLBACK_EXECUTE;
            default:
                throw new Exception("not match job execute status");
        }
    }

    public static NodeExecuteStatusEnum getNodeNodeExecuteStatus(EventStatusEnum eventStatusEnum, NodeExecuteStatusEnum originNodeExecuteStatus) {
        switch (originNodeExecuteStatus) {
            case UN_NODE_EXECUTE:
            case IN_NODE_EXECUTE:
                if (EventStatusEnum.FAIL_EVENT_EXECUTE.equals(eventStatusEnum)) {
                    return NodeExecuteStatusEnum.FAIL_NODE_EXECUTE;
                }

                if (EventStatusEnum.UN_EVENT_EXECUTE.equals(eventStatusEnum)) {
                    return NodeExecuteStatusEnum.UN_NODE_EXECUTE;
                }

                if (EventStatusEnum.IN_EVENT_EXECUTE.equals(eventStatusEnum)) {
                    return NodeExecuteStatusEnum.IN_NODE_EXECUTE;
                }

                if (EventStatusEnum.SKIPPED.equals(eventStatusEnum)) {
                    return NodeExecuteStatusEnum.SKIPPED;
                }

                if (EventStatusEnum.SUCCEED_EVENT_EXECUTE.equals(eventStatusEnum)) {
                    return NodeExecuteStatusEnum.SUCCEED_NODE_EXECUTE;
                }
            case RECOVERY_UN_NODE_EXECUTE:
            case RECOVERY_IN_NODE_EXECUTE:
                if (EventStatusEnum.FAIL_EVENT_EXECUTE.equals(eventStatusEnum)) {
                    return NodeExecuteStatusEnum.FAIL_NODE_EXECUTE;
                }

                if (EventStatusEnum.UN_EVENT_EXECUTE.equals(eventStatusEnum)) {
                    return NodeExecuteStatusEnum.RECOVERY_UN_NODE_EXECUTE;
                }

                if (EventStatusEnum.IN_EVENT_EXECUTE.equals(eventStatusEnum)) {
                    return NodeExecuteStatusEnum.RECOVERY_IN_NODE_EXECUTE;
                }

                if (EventStatusEnum.SKIPPED.equals(eventStatusEnum)) {
                    return NodeExecuteStatusEnum.SKIPPED;
                }

                if (EventStatusEnum.SUCCEED_EVENT_EXECUTE.equals(eventStatusEnum)) {
                    return NodeExecuteStatusEnum.SUCCEED_NODE_EXECUTE;
                }

            case UN_NODE_RETRY_EXECUTE:
            case IN_NODE_RETRY_EXECUTE:
                if (EventStatusEnum.FAIL_EVENT_EXECUTE.equals(eventStatusEnum)) {
                    return NodeExecuteStatusEnum.FAIL_NODE_RETRY_EXECUTE;
                }

                if (EventStatusEnum.IN_EVENT_EXECUTE.equals(eventStatusEnum)) {
                    return NodeExecuteStatusEnum.IN_NODE_RETRY_EXECUTE;
                }

                if (EventStatusEnum.SKIPPED.equals(eventStatusEnum)) {
                    return NodeExecuteStatusEnum.SKIPPED;
                }

                if (EventStatusEnum.SUCCEED_EVENT_EXECUTE.equals(eventStatusEnum)) {
                    return NodeExecuteStatusEnum.SUCCEED_NODE_RETRY_EXECUTE;
                }

                if (EventStatusEnum.UN_EVENT_EXECUTE.equals(eventStatusEnum)) {
                    return NodeExecuteStatusEnum.UN_NODE_RETRY_EXECUTE;
                }

            case RECOVERY_UN_NODE_RETRY_EXECUTE:
            case RECOVERY_IN_NODE_RETRY_EXECUTE:
                if (EventStatusEnum.FAIL_EVENT_EXECUTE.equals(eventStatusEnum)) {
                    return NodeExecuteStatusEnum.FAIL_NODE_RETRY_EXECUTE;
                }

                if (EventStatusEnum.IN_EVENT_EXECUTE.equals(eventStatusEnum)) {
                    return NodeExecuteStatusEnum.RECOVERY_IN_NODE_RETRY_EXECUTE;
                }

                if (EventStatusEnum.SKIPPED.equals(eventStatusEnum)) {
                    return NodeExecuteStatusEnum.SKIPPED;
                }

                if (EventStatusEnum.SUCCEED_EVENT_EXECUTE.equals(eventStatusEnum)) {
                    return NodeExecuteStatusEnum.SUCCEED_NODE_RETRY_EXECUTE;
                }

                if (EventStatusEnum.UN_EVENT_EXECUTE.equals(eventStatusEnum)) {
                    return NodeExecuteStatusEnum.RECOVERY_UN_NODE_RETRY_EXECUTE;
                }

            case UN_NODE_ROLLBACK_EXECUTE:
            case IN_NODE_ROLLBACK_EXECUTE:
                if (EventStatusEnum.FAIL_EVENT_EXECUTE.equals(eventStatusEnum)) {
                    return NodeExecuteStatusEnum.FAIL_NODE_ROLLBACK_EXECUTE;
                }

                if (EventStatusEnum.IN_EVENT_EXECUTE.equals(eventStatusEnum)) {
                    return NodeExecuteStatusEnum.IN_NODE_ROLLBACK_EXECUTE;
                }

                if (EventStatusEnum.SKIPPED.equals(eventStatusEnum)) {
                    return NodeExecuteStatusEnum.SKIPPED;
                }

                if (EventStatusEnum.SUCCEED_EVENT_EXECUTE.equals(eventStatusEnum)) {
                    return NodeExecuteStatusEnum.SUCCEED_NODE_ROLLBACK_EXECUTE;
                }

                if (EventStatusEnum.UN_EVENT_EXECUTE.equals(eventStatusEnum)) {
                    return NodeExecuteStatusEnum.UN_NODE_ROLLBACK_EXECUTE;
                }
            case RECOVERY_UN_NODE_ROLLBACK_EXECUTE:
            case RECOVERY_IN_NODE_ROLLBACK_EXECUTE:
                if (EventStatusEnum.FAIL_EVENT_EXECUTE.equals(eventStatusEnum)) {
                    return NodeExecuteStatusEnum.FAIL_NODE_ROLLBACK_EXECUTE;
                }

                if (EventStatusEnum.IN_EVENT_EXECUTE.equals(eventStatusEnum)) {
                    return NodeExecuteStatusEnum.RECOVERY_IN_NODE_ROLLBACK_EXECUTE;
                }

                if (EventStatusEnum.SKIPPED.equals(eventStatusEnum)) {
                    return NodeExecuteStatusEnum.SKIPPED;
                }

                if (EventStatusEnum.SUCCEED_EVENT_EXECUTE.equals(eventStatusEnum)) {
                    return NodeExecuteStatusEnum.SUCCEED_NODE_ROLLBACK_EXECUTE;
                }

                if (EventStatusEnum.UN_EVENT_EXECUTE.equals(eventStatusEnum)) {
                    return NodeExecuteStatusEnum.RECOVERY_UN_NODE_ROLLBACK_EXECUTE;
                }
            default:
                throw new RuntimeException("not match originNodeExecuteStatus");
        }
    }
}
