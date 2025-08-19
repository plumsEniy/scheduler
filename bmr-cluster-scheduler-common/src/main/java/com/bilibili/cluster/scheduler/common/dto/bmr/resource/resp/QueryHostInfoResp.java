package com.bilibili.cluster.scheduler.common.dto.bmr.resource.resp;

import com.bilibili.cluster.scheduler.common.dto.bmr.resource.BaseResourcePage;
import com.bilibili.cluster.scheduler.common.dto.bmr.resource.model.ResourceHostInfo;
import com.bilibili.cluster.scheduler.common.response.BaseMsgResp;
import lombok.Data;

/**
 * @description: 查询主机信息
 * @Date: 2024/5/23 09:04
 * @Author: nizhiqiang
 */

@Data
public class QueryHostInfoResp extends BaseMsgResp {

    BaseResourcePage<ResourceHostInfo> obj;
}
