package com.bilibili.cluster.scheduler.common.enums.flow;

import com.bilibili.cluster.scheduler.common.enums.host.HostJobStateEnum;

import java.util.ArrayList;
import java.util.List;

/**
 * 发布单状态
 */
public enum FlowStatusEnum {

    UNDER_APPROVAL("审批中"),
    APPROVAL_NOT_PASS("审批未通过"),
    APPROVAL_PASS("审批通过"),
    APPROVAL_FAIL("审批失败"),

    PREPARE_EXECUTE("准备发布"),
    PREPARE_EXECUTE_FAILED("准备发布-失败"),

    UN_EXECUTE("待执行"),
    IN_EXECUTE("执行中"),
    SUCCEED_EXECUTE("执行成功"),
    FAIL_EXECUTE("执行失败"),
    PAUSED("暂停"),
    TERMINATE("结单"),
    CANCEL("取消"),

    // 支持回滚
    IN_ROLLBACK("回滚中"),
    ROLLBACK_PAUSED("暂停回滚"),
    ROLLBACK_FAILED("回滚失败"),
    ROLLBACK_SUCCESS("回滚完成"),
    ;

    private String desc;

    public static List<FlowStatusEnum> runningStatusList = new ArrayList<FlowStatusEnum>() {{
        add(IN_EXECUTE);
        add(SUCCEED_EXECUTE);
        add(FAIL_EXECUTE);
        add(PAUSED);
        add(IN_ROLLBACK);
        add(ROLLBACK_PAUSED);
        add(ROLLBACK_SUCCESS);
        add(ROLLBACK_FAILED);
    }};

    public static boolean canCancel(FlowStatusEnum flowStatus) {
        switch (flowStatus) {
            case UNDER_APPROVAL:
            case APPROVAL_NOT_PASS:
            case APPROVAL_FAIL:
            case APPROVAL_PASS:
                return true;
            default:
                return false;
        }
    }

    FlowStatusEnum(String desc) {
        this.desc = desc;
    }

    public String getDesc() {
        return desc;
    }


    public static boolean isPause(FlowStatusEnum flowStatus) {
        switch (flowStatus) {
            case PAUSED:
                return true;
            default:
                return false;
        }
    }

    public static boolean isFinish(FlowStatusEnum flowStatus) {
        switch (flowStatus) {
            case CANCEL:
            case SUCCEED_EXECUTE:
            case TERMINATE:
                return true;
            default:
                return false;
        }

    }

    public static HostJobStateEnum convertFlowStatusToHostJobStatus(FlowStatusEnum flowStatus) {
        switch (flowStatus) {
            case UNDER_APPROVAL:
                return HostJobStateEnum.IN_APPROVAL;
            case APPROVAL_PASS:
            case UN_EXECUTE:
                return HostJobStateEnum.UN_EXECUTE;
            case APPROVAL_FAIL:
            case APPROVAL_NOT_PASS:
            case TERMINATE:
            case CANCEL:
                return HostJobStateEnum.ONLINE;
            case PAUSED:
            case IN_EXECUTE:
                return HostJobStateEnum.OFFLINE_IN;
            case SUCCEED_EXECUTE:
                return HostJobStateEnum.OFFLINE_SUCCESS;
            case FAIL_EXECUTE:
                return HostJobStateEnum.OFFLINE_FAILURE;
            default:
                throw new IllegalArgumentException("un handler flow status status to host job status, flow status is " + flowStatus);
        }
    }

    public boolean isRunning() {
        return runningStatusList.contains(this);
    }

    public FlowStatusEnum generateNextStatus(FlowOperateButtonEnum operate) {
        switch (this) {
            case IN_EXECUTE:
                return handleInExecuteOperate(operate);
            case SUCCEED_EXECUTE:
                return handleSuccessExecuteOperate(operate);
            case PAUSED:
                return handlePauseOperate(operate);
            case FAIL_EXECUTE:
                return handleFailOperate(operate);
            case IN_ROLLBACK:
                return handleInRollbackOperate(operate);
            case ROLLBACK_PAUSED:
                return handlerRollbackPauseOperate(operate);
            case ROLLBACK_FAILED:
                return handlerRollbackFailedOperate(operate);
            case ROLLBACK_SUCCESS:
                return handlerRollbackSuccessOperate(operate);
            default:
                return null;
        }
    }

    private FlowStatusEnum handleFailOperate(FlowOperateButtonEnum operate) {
        switch (operate) {
            case SKIP_FAILED_AND_PROCESS:
                return IN_EXECUTE;
            case TERMINATE:
                return TERMINATE;
            case FULL_ROLLBACK:
            case STAGED_ROLLBACK:
                return IN_ROLLBACK;
            default:
                return null;
        }
    }


    public static FlowStatusEnum handlePauseOperate(FlowOperateButtonEnum operate) {
        switch (operate) {
            case TERMINATE:
                return TERMINATE;
            case PROCEED:
                return IN_EXECUTE;
            case FULL_ROLLBACK:
            case STAGED_ROLLBACK:
                return IN_ROLLBACK;
            default:
                return null;
        }
    }

    public static FlowStatusEnum handleSuccessExecuteOperate(FlowOperateButtonEnum operate) {
        switch (operate) {
            case TERMINATE:
                return TERMINATE;
            case FULL_ROLLBACK:
            case STAGED_ROLLBACK:
                return IN_ROLLBACK;
            default:
                return null;
        }
    }

    public static FlowStatusEnum handleInExecuteOperate(FlowOperateButtonEnum operate) {
        switch (operate) {
            case PAUSE:
                return PAUSED;
            default:
                return null;
        }
    }

    public boolean isPause() {
        return this == PAUSED;
    }

    public boolean isFinish() {
        switch (this) {
            case CANCEL:
            case TERMINATE:
                return true;
            default:
                return false;
        }

    }

    private FlowStatusEnum handleInRollbackOperate(FlowOperateButtonEnum operate) {
        switch (operate) {
            case PAUSE:
                return FlowStatusEnum.ROLLBACK_PAUSED;
            default:
                return null;
        }
    }

    private FlowStatusEnum handlerRollbackPauseOperate(FlowOperateButtonEnum operate) {
        switch (operate) {
            case FULL_ROLLBACK:
            case STAGED_ROLLBACK:
                return FlowStatusEnum.IN_ROLLBACK;
            case TERMINATE:
                return TERMINATE;
            default:
                return null;
        }
    }

    private FlowStatusEnum handlerRollbackFailedOperate(FlowOperateButtonEnum operate) {
        switch (operate) {
            case SKIP_FAILED_AND_PROCESS:
                return IN_ROLLBACK;
            case TERMINATE:
                return TERMINATE;
            default:
                return null;
        }
    }

    private FlowStatusEnum handlerRollbackSuccessOperate(FlowOperateButtonEnum operate) {
        switch (operate) {
            case TERMINATE:
                return TERMINATE;
            default:
                return null;
        }
    }
}
