package com.bilibili.cluster.scheduler.common.dto.bmr.metadata.resp;

import com.bilibili.cluster.scheduler.common.dto.bmr.resource.model.HostAndLogicGroupInfo;
import com.bilibili.cluster.scheduler.common.response.BaseMsgResp;
import lombok.Data;

import java.util.Map;

/**
 * @description:
 * @Date: 2024/5/15 15:35
 * @Author: nizhiqiang
 */

@Data
public class QueryNodeGroupInfoResp extends BaseMsgResp {

    private Map<String, HostAndLogicGroupInfo> obj;

}
