package com.bilibili.cluster.scheduler.common.dto.oa.req;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.http.HttpMethod;

/**
 * @description: 获取token
 * @Date: 2024/3/6 14:09
 * @Author: nizhiqiang
 */

@Data
@AllArgsConstructor
public class GetOATokenReq {
    private String appId;

    private String appSecret;
}
