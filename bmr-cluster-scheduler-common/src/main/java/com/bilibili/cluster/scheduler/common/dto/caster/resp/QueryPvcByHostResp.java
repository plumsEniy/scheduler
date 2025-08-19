package com.bilibili.cluster.scheduler.common.dto.caster.resp;

import com.bilibili.cluster.scheduler.common.dto.caster.PvcInfo;
import lombok.Data;

import java.util.List;

/**
 * @description: 查询pod信息
 * @Date: 2024/7/22 11:08
 * @Author: nizhiqiang
 */

@Data
public class QueryPvcByHostResp extends BaseComResp {

    private List<PvcInfo> data;
}
