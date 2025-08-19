package com.bilibili.cluster.scheduler.common.dto.caster.resp;

import com.bilibili.cluster.scheduler.common.dto.caster.NodeInfoListData;
import lombok.Data;

@Data
public class QueryNodeInfoListResp extends BaseComResp {

    private NodeInfoListData data;

}
