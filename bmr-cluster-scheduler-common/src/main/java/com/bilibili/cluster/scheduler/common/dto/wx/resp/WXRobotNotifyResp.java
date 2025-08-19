package com.bilibili.cluster.scheduler.common.dto.wx.resp;

import com.bilibili.cluster.scheduler.common.response.BaseResp;
import lombok.Data;

/**
 * @description: 微信机器人通知
 * @Date: 2024/3/19 20:06
 * @Author: nizhiqiang
 */
@Data
public class WXRobotNotifyResp extends BaseResp {
    private int errcode;
    private String errmsg;
}
