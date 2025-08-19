package com.bilibili.cluster.scheduler.common.dto.bmr.metadata.resp;

import com.bilibili.cluster.scheduler.common.dto.bmr.metadata.MetadataComponentData;
import com.bilibili.cluster.scheduler.common.response.BaseMsgResp;
import lombok.Data;

import java.util.List;

/**
 * @description: 查询组件信息
 * @Date: 2024/5/15 14:45
 * @Author: nizhiqiang
 */

@Data
public class QueryMetadataComponentResp extends BaseMsgResp {
    private MetadataComponentData obj;

}
