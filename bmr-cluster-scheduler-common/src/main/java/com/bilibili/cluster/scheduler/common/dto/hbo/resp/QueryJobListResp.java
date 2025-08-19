package com.bilibili.cluster.scheduler.common.dto.hbo.resp;

import com.bilibili.cluster.scheduler.common.dto.hbo.model.HboJobInfo;
import com.bilibili.cluster.scheduler.common.response.BaseMsgResp;
import lombok.Data;

import java.util.List;

/**
 * @description: 根据jobid查询
 * @Date: 2024/12/26 14:13
 * @Author: nizhiqiang
 */

@Data
public class QueryJobListResp extends BaseMsgResp {
    List<HboJobInfo> data;
}
