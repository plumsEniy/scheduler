package com.bilibili.cluster.scheduler.common.dto.caster.resp;

import lombok.Data;

/**
 * @description:
 * @Date: 2024/3/11 17:50
 * @Author: nizhiqiang
 */
@Data
public class RemoveK8sNodeLabelResp extends BaseComResp {
    String data;
}