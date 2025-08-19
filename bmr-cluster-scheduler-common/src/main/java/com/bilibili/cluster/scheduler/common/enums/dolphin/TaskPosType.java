package com.bilibili.cluster.scheduler.common.enums.dolphin;


import lombok.Getter;

@Getter
public enum TaskPosType {

    DOLPHIN_SINGLE_NODE("dolphin单节点", true,false, true),
    DOLPHIN_START_NODE("dolphin开始节点", true, false, false),
    DOLPHIN_INTERMEDIATE_NODE("dolphin中间节点",false, true, false),
    DOLPHIN_END_NODE("dolphin结束节点", false, false, true),
    ;

    private String desc;
    private boolean isStartNode;
    private boolean isIntermediateNode;
    private boolean isEndNode;

    TaskPosType(String desc, boolean isStartNode, boolean isIntermediateNode, boolean isEndNode) {
        this.desc = desc;
        this.isStartNode = isStartNode;
        this.isIntermediateNode = isIntermediateNode;
        this.isEndNode = isEndNode;
    }
}
