package com.bilibili.cluster.scheduler.common.dto.bmr.resourceV2.resp;

import com.bilibili.cluster.scheduler.common.dto.bmr.resourceV2.model.ResourceLogicGroup;
import com.bilibili.cluster.scheduler.common.response.BaseMsgResp;
import lombok.Data;

import java.util.List;

@Data
public class QueryLogicGroupInfoResp extends BaseMsgResp {

    private List<ResourceLogicGroup> obj;
}
