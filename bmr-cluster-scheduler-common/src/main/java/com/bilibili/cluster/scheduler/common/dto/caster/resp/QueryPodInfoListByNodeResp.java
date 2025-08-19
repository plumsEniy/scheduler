package com.bilibili.cluster.scheduler.common.dto.caster.resp;

import com.bilibili.cluster.scheduler.common.dto.caster.PodInfoListByNodeData;
import lombok.Data;

@Data
public class QueryPodInfoListByNodeResp extends BaseComResp {

    private PodInfoListByNodeData data;

}
