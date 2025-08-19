package com.bilibili.cluster.scheduler.common.dto.oa.resp;

import com.bilibili.cluster.scheduler.common.dto.oa.SubmitOAFormData;
import lombok.Data;

/**
 * @description: 查询oa
 * @Date: 2024/3/6 15:41
 * @Author: nizhiqiang
 */
@Data
public class SubmitOAFormResp<T> extends BaseOAResp {
    private SubmitOAFormData<T> data;
}
