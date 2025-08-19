package com.bilibili.cluster.scheduler.common.enums.jobAgent;

import java.util.HashSet;
import java.util.Set;

/**
 * @description: 脚本状态的枚举
 * @Date: 2024/5/10 11:07
 * @Author: nizhiqiang
 */
public enum ScriptJobExecuteStateEnum {
    INIT(0),
    RUNNING(1),
    SUCCESS(2),
    FAILURE(3),
    TIMEOUT(7);

    private final static Set<ScriptJobExecuteStateEnum> doneSet = new HashSet<>();

    private final Integer code;

    public Integer getCode() {
        return this.code;
    }

    ScriptJobExecuteStateEnum(Integer code) {
        this.code = code;
    }

    static {
        doneSet.add(SUCCESS);
        doneSet.add(FAILURE);
        doneSet.add(TIMEOUT);
    }

    public Boolean executeDone() {
        return doneSet.contains(this);
    }

    public static ScriptJobExecuteStateEnum convertCodeToState(Integer code) {
        for (ScriptJobExecuteStateEnum jobExecuteState : ScriptJobExecuteStateEnum.values()) {
            if (jobExecuteState.getCode().equals(code)) {
                return jobExecuteState;
            }
        }
        throw new IllegalArgumentException("unknown job state code:" + code);
    }
}
