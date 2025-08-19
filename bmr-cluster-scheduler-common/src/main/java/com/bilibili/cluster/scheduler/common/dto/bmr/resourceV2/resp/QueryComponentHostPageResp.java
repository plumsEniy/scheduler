package com.bilibili.cluster.scheduler.common.dto.bmr.resourceV2.resp;

import com.bilibili.cluster.scheduler.common.dto.bmr.resourceV2.PageInfo;
import com.bilibili.cluster.scheduler.common.dto.bmr.resourceV2.model.ComponentHostRelationModel;
import com.bilibili.cluster.scheduler.common.response.BaseMsgResp;
import lombok.Data;

/**
 * @description:
 * @Date: 2024/9/4 16:45
 * @Author: nizhiqiang
 */
@Data
public class QueryComponentHostPageResp extends BaseMsgResp {
    private PageInfo<ComponentHostRelationModel> obj;
}
