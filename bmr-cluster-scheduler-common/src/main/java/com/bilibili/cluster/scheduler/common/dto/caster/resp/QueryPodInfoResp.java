package com.bilibili.cluster.scheduler.common.dto.caster.resp;

import com.bilibili.cluster.scheduler.common.dto.caster.PodInfo;
import lombok.Data;

import java.util.List;

/**
 * @description: 查询pod信息
 * @Date: 2024/7/22 11:08
 * @Author: nizhiqiang
 */

@Data

public class QueryPodInfoResp  extends BaseComResp {
    private List<PodInfo> data;
}
