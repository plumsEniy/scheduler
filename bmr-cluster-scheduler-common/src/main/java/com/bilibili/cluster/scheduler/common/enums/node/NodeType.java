package com.bilibili.cluster.scheduler.common.enums.node;

import lombok.Getter;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public enum NodeType {

    NORMAL(true, "普通可执行节点"),

    STAGE_START_NODE(false, "阶段开始节点"),

    STAGE_END_NODE(false, "阶段结束节点"),

//    额外节点不会触发资源管理系统的刷新逻辑
    EXTRA_NODE(true, "额外节点"),

    ;

    @Getter
    boolean isNormalNode;

    @Getter
    String desc;

    NodeType(boolean isNormalNode, String desc) {
        this.isNormalNode = isNormalNode;
        this.desc = desc;
    }

    // 是否为业务处理节点
    public boolean isNormalExecNode() {
        return isNormalNode;
    }

    public static List<NodeType> getNormalExecNodeList() {
        return Arrays.asList(NodeType.values()).stream().filter(NodeType::isNormalNode).collect(Collectors.toList());
    }


}
