package com.bilibili.cluster.scheduler.common.dto.wx.req;

import cn.hutool.core.annotation.Alias;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * @description: 微信机器人通知
 * @Date: 2024/3/19 20:02
 * @Author: nizhiqiang
 */

@Data
public class WXRobotNotifyReq {

    @Alias("msgtype")
    private String msgType = "markdown";

    private ContentText markdown;

    public WXRobotNotifyReq(String context) {
        this.markdown = new ContentText(context);
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    class ContentText implements Serializable {
        private String content;
    }
}
