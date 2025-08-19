package com.bilibili.cluster.scheduler.common.dto.bmr.resource.resp;

import com.bilibili.cluster.scheduler.common.response.BaseMsgResp;
import lombok.Data;

/**
 * @description: 移除yarn标签
 * @Date: 2024/3/11 19:37
 * @Author: nizhiqiang
 */
@Data
public class RemoveYarnLabelResp extends BaseMsgResp {
    private boolean obj;
}
