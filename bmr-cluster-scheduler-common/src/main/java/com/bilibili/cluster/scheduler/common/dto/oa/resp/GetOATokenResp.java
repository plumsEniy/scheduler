package com.bilibili.cluster.scheduler.common.dto.oa.resp;

import lombok.Data;

import java.util.Map;

/**
 * @description: 获取token
 * @Date: 2024/3/6 14:12
 * @Author: nizhiqiang
 */
@Data
public class GetOATokenResp extends BaseOAResp {
    private Map<String,String> data;
}
