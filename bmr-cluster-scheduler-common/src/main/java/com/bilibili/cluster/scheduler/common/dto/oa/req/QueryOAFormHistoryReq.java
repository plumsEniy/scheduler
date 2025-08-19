package com.bilibili.cluster.scheduler.common.dto.oa.req;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.http.HttpMethod;

/**
 * @description: 查询oa历史的请求
 * @Date: 2024/3/6 14:25
 * @Author: nizhiqiang
 */

@Data
@AllArgsConstructor
public class QueryOAFormHistoryReq {
    private String orderId;
}
