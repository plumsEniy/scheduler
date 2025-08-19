package com.bilibili.cluster.scheduler.common.dto.button;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum StageStateEnum {

    SUCCESS("成功"),
    RUNNING("运行中"),
    UN_EXECUTE("未运行"),
    FAIL("失败"),
    ;

    String desc;


    public boolean isFinishState() {
        return SUCCESS == this || FAIL == this;
    }

}
