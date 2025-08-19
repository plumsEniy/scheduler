package com.bilibili.cluster.scheduler.common.dto.oa.req;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.http.HttpMethod;

/**
 * @description: 查询oa审批单
 * @Date: 2024/3/6 10:59
 * @Author: nizhiqiang
 */
@Data
@AllArgsConstructor
public class QueryOAFormReq {

    private String orderId;
}
