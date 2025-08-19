package com.bilibili.cluster.scheduler.common.dto.bmr.metadata.resp;

import com.bilibili.cluster.scheduler.common.dto.bmr.metadata.MetadataComponentData;
import com.bilibili.cluster.scheduler.common.response.BaseMsgResp;
import lombok.Data;

import java.util.List;

/**
 * @description:
 * @Date: 2024/5/14 16:37
 * @Author: nizhiqiang
 */

@Data
public class QueryComponentListResp extends BaseMsgResp {
    private List<MetadataComponentData> obj;
}
