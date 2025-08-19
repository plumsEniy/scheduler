package com.bilibili.cluster.scheduler.common.dto.spark.plus;

public enum ExperimentExecutionStatus {

    INIT,
    SUBMIT_FAILURE,
    UNEXECUTED,
    CANCEL,
    SUBMITTED_SUCCESS,
    RUNNING_EXECUTION,
    READY_PAUSE,
    PAUSE,
    READY_STOP,
    STOP,
    FAILURE,
    SUCCESS,
    DELAY_EXECUTION,
    SERIAL_WAIT,
    READY_BLOCK,
    BLOCK,
    WAITING_COST, // 异步等待cost指标计算
    DQC_CRC32_DIFF, // dqc crc32 未通过
    DQC_COUNT_DIFF, // dqc count 未通过
    ;

    public static boolean isSuccess(String executionStatus) {
        if (SUCCESS.name().equals(executionStatus)) {
            return true;
        }
        return false;
    }

    public static boolean isFailure(String executionStatus) {
        if (FAILURE.name().equals(executionStatus)) {
            return true;
        }
        if (STOP.name().equals(executionStatus)) {
            return true;
        }
        if (CANCEL.name().equals(executionStatus)) {
            return true;
        }
        if (DQC_COUNT_DIFF.name().equals(executionStatus)) {
            return true;
        }
        if (DQC_CRC32_DIFF.name().equals(executionStatus)) {
            return true;
        }

        return false;
    }

    public static boolean isWaitingCost(String executionStatus) {
        if (WAITING_COST.name().equals(executionStatus)) {
            return true;
        }
        return false;
    }

    public static boolean isRunning(String executionStatus) {
        if (RUNNING_EXECUTION.name().equals(executionStatus)) {
            return true;
        }
        return false;
    }

}
