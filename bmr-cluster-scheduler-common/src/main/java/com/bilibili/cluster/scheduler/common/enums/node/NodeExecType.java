package com.bilibili.cluster.scheduler.common.enums.node;


import lombok.Getter;

public enum NodeExecType {

    // 状态切换
    //          初始状态
    //          FORWARD   <===   WAITING_FORWARD
    //            //                 /｜|\
    //           //                    \\
    //         \｜|/                    \\
    //    WAITING_ROLLBACK   ===>    ROLLBACK

    WAITING_FORWARD(true, "等待正常执行"),

    FORWARD(false, "正常执行状态"),

    WAITING_ROLLBACK(false, "等待回滚"),

    ROLLBACK(true, "回滚状态"),
    ;

    @Getter
    private boolean isRollback;

    @Getter
    private String desc;

    NodeExecType(boolean isRollback, String desc) {
        this.isRollback = isRollback;
        this.desc = desc;
    }

    public boolean isRollbackState() {
        return isRollback;
    }

    public boolean isNormalForwardState() {
        return FORWARD.equals(this);
    }

}
