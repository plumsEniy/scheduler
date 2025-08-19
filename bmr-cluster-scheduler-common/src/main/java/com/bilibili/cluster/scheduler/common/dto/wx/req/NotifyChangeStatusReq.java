package com.bilibili.cluster.scheduler.common.dto.wx.req;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * @description:
 * @Date: 2024/3/18 19:10
 * @Author: nizhiqiang
 */
@Data
@AllArgsConstructor
public class NotifyChangeStatusReq {
    private long id;
    private String process_status_str;
    private String checker;
}
