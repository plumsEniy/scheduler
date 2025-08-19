package com.bilibili.cluster.scheduler.common.enums.event;


public enum EventStatusEnum {

    // 绿色, 特指该节点不需要继续执行
    SKIPPED("跳过"),
    // 灰色
    UN_EVENT_EXECUTE("待执行"),
    // info
    IN_EVENT_EXECUTE("执行中"),
    // success
    SUCCEED_EVENT_EXECUTE("执行成功"),
    // failed
    FAIL_EVENT_EXECUTE("执行失败"),
    // 绿色，特指该事件不需要执行，后续事件还需执行
    EVENT_SKIPPED("事件级别跳过"),
    ;

    private String desc;

    EventStatusEnum(String desc) {
        this.desc = desc;
    }

    public boolean isFinish() {
        switch (this) {
            case SKIPPED:
            case SUCCEED_EVENT_EXECUTE:
            case FAIL_EVENT_EXECUTE:
            case EVENT_SKIPPED:
                return true;
            default:
                return false;
        }
    }

    public boolean isSuccess() {
        if (this.equals(SUCCEED_EVENT_EXECUTE)) {
            return true;
        }

        return false;
    }

    public boolean isFailed() {
        if (this.equals(FAIL_EVENT_EXECUTE)) {
            return true;
        }
        return false;
    }

}
