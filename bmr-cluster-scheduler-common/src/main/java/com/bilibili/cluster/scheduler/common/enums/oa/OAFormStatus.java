package com.bilibili.cluster.scheduler.common.enums.oa;

/**
 * @description: oa表单状态
 * @Date: 2024/3/6 14:22
 * @Author: nizhiqiang
 */
public enum OAFormStatus {
    UNDER_APPROVAL, // 审批中 or 待我处理
    APPROVED,       // 已完结（审批通过）
    DISCARDED,      // 已废弃

    NOT_ACCESSABLE,      // 无法访问

//    CANCEL_DEPLOY,      //      取消发布
    ;

    public static boolean isPass(String applyState) {
        return APPROVED.name().equals(applyState);
    }

}
