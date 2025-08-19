package com.bilibili.cluster.scheduler.common.enums.host;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @description: 主机上任务上下线状态
 * @Date: 2024/3/15 16:30
 * @Author: nizhiqiang
 */
@AllArgsConstructor
public enum HostJobStateEnum {
    /**
     * 上线
     */
    ONLINE("上线"),

    /**
     * 审批中
     */
    IN_APPROVAL("审批中"),

    /**
     * 待执行
     */
    UN_EXECUTE("待执行"),

    /**
     * 下线成功
     */
    OFFLINE_SUCCESS("下线成功"),

    /**
     * 下线中
     */
    OFFLINE_IN("下线中"),

    /**
     * 下线失败
     */
    OFFLINE_FAILURE("下线失败"),
    ;

    @Getter
    private String desc;


    public boolean isOnline() {
        switch (this) {
            case ONLINE:
                return true;
            default:
                return false;
        }
    }
}
