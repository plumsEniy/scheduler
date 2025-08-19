package com.bilibili.cluster.scheduler.common.dto.bmr.metadata.resp;

import com.bilibili.cluster.scheduler.common.dto.bmr.metadata.MetadataClusterData;
import com.bilibili.cluster.scheduler.common.response.BaseMsgResp;
import lombok.Data;

/**
 * @description: 查询集群详情
 * @Date: 2024/5/23 09:01
 * @Author: nizhiqiang
 */
@Data
public class QueryClusterDetailResp extends BaseMsgResp {
    private MetadataClusterData obj;
}