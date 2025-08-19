package com.bilibili.cluster.scheduler.common.dto.wx.resp;

import com.bilibili.cluster.scheduler.common.response.BaseResp;
import lombok.Data;

import java.util.Map;

/**
 * @description:
 * @Date: 2024/3/20 14:47
 * @Author: nizhiqiang
 */
@Data
public class WxPushMsgResp extends BaseResp {
    private Map<String, String> data;
}
