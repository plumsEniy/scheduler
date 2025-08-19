package com.bilibili.cluster.scheduler.common.dto.caster;

import lombok.Data;

import java.util.List;

@Data
public class NodeInfoListData {

    private List<ResourceNodeInfo> items;

}
