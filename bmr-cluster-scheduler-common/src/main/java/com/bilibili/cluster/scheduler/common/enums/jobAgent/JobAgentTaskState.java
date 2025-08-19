package com.bilibili.cluster.scheduler.common.enums.jobAgent;

import com.bilibili.cluster.scheduler.common.enums.event.EventStatusEnum;
import lombok.Getter;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @description:
 * @Date: 2024/5/16 11:42
 * @Author: nizhiqiang
 */
public enum JobAgentTaskState {
    /**
     * job-agent 状态码列表
     * TaskStatus_Wait       TaskStatus = 0
     * TaskStatus_Running    TaskStatus = 1
     * TaskStatus_Success    TaskStatus = 2
     * TaskStatus_Fail       TaskStatus = 3
     * TaskStatus_Cancel     TaskStatus = 4
     * TaskStatus_Killed     TaskStatus = 5
     * TaskStatus_Prepare    TaskStatus = 6
     * TaskStatus_Timout     TaskStatus = 7
     * TaskStatus_System_Err TaskStatus = 8
     * TaskStatus_Retrying   TaskStatus = 9
     * TaskStatus_Killing    TaskStatus = 10
     * TaskStatus_Wait_Retry TaskStatus = 11
     */

    WAIT(0),
    RUNNING(1),
    SUCCESS(2),
    FAILED(3),
    CANCEL(4),
    KILLED(5),
    PREPARE(6),
    TIMEOUT(7),
    SYSTEM_ERR(8),
    RETRYING(9),
    KILLING(10),
    WAIT_RETRY(11),
    UNKNOWN(99);


    @Getter
    int code;

    JobAgentTaskState(int code) {
        this.code = code;
    }

    public static boolean isRunning(int state) {
        return RUNNING.code == state;
    }

    public static boolean isSuccess(int state) {
        return SUCCESS.code == state;
    }

    private static Map<Integer, JobAgentTaskState> VALUE_MAP = Arrays.stream(values())
            .collect(Collectors.toMap(JobAgentTaskState::getCode, Function.identity()));

    public static JobAgentTaskState getByCode(int code) {
        return VALUE_MAP.getOrDefault(code, UNKNOWN);
    }

    public static boolean isFailed(int state) {
        if (FAILED.code == state) return true;
        if (TIMEOUT.code == state) return true;
        if (SYSTEM_ERR.code == state) return true;
        if (CANCEL.code == state) return true;

        return false;
    }

    public static boolean isStateFinish(int state) {
        return !isStateRunning(state);
    }

    public static boolean isStateRunning(int state) {
        if (WAIT.code == state) return true;
        if (RUNNING.code == state) return true;
        if (RETRYING.code == state) return true;
        return false;
    }

    public static EventStatusEnum transferToStatus(int state) {
        if (isStateRunning(state)) return EventStatusEnum.IN_EVENT_EXECUTE;
        if (SUCCESS.code == state) return EventStatusEnum.SUCCEED_EVENT_EXECUTE;
        if (isFailed(state)) return EventStatusEnum.FAIL_EVENT_EXECUTE;
        return EventStatusEnum.UN_EVENT_EXECUTE;
    }

}
