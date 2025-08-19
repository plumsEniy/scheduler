package com.bilibili.cluster.scheduler.common.dto.caster;

import cn.hutool.core.annotation.Alias;
import lombok.Data;

import java.util.List;

@Data
public class NodeTaint {

    @Alias(value = "node_name")
    private String nodeName;

    @Alias(value = "taint_ops")
    private List<TaintOption> taintOptions;

}
