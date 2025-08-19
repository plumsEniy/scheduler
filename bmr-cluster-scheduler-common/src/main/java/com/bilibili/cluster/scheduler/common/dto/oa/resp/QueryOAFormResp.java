package com.bilibili.cluster.scheduler.common.dto.oa.resp;

import com.bilibili.cluster.scheduler.common.dto.oa.QueryOAFormData;
import lombok.Data;

/**
 * @description: 查询oa审批单响应
 * @Date: 2024/3/6 11:01
 * @Author: nizhiqiang
 */

@Data
public class QueryOAFormResp<T> extends BaseOAResp {
    private QueryOAFormData<T> data;

}
