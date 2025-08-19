package com.bilibili.cluster.scheduler.common.dto.wx.req;

import cn.hutool.core.annotation.Alias;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

/**
 * @description:
 * @Date: 2024/3/20 14:45
 * @Author: nizhiqiang
 */
@Data
@AllArgsConstructor
public class WXPushMsgReq {
    @Alias(value = "userlist")
    private List<String> userList;

    @Alias(value = "msg_type")
    private String msgType;

    private String content;
}
