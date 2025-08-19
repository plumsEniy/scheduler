package com.bilibili.cluster.scheduler.common.dto.caster.resp;

import lombok.Data;

/**
 * @description: caster的响应
 * @Date: 2024/7/10 15:20
 * @Author: nizhiqiang
 */

@Data
public class BaseComResp {
    Integer status;
    String message;
}
