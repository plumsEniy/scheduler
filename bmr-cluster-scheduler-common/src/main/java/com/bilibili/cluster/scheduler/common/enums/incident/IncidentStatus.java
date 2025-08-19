package com.bilibili.cluster.scheduler.common.enums.incident;

import lombok.AllArgsConstructor;
import lombok.Getter;

/** 变更状态
 * @author sgh
 * @date 2022-02-09
 */

@AllArgsConstructor
@Getter
public enum IncidentStatus {
    /**
     * 初始化状态仅用于bmr表示状态
     */
    INIT("初始化", -1),
    FINISH("已完成", 7),
    FAIL("失败", 6),
    ROLLBACK("已回滚", 8),
    GIVE_UP("放弃", 4),
    ;
    private String desc;
    private int num;
}