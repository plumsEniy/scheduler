package com.bilibili.cluster.scheduler.common.dto.caster.req;

import cn.hutool.core.annotation.Alias;
import com.bilibili.cluster.scheduler.common.dto.caster.NodeTaint;
import lombok.Data;

import java.util.List;

@Data
public class CasterNodeTaintReq {

    @Alias(value = "cluster_id")
    private long clusterId;

    @Alias(value = "node_taint_list")
    private List<NodeTaint> nodeTaintList;

}
