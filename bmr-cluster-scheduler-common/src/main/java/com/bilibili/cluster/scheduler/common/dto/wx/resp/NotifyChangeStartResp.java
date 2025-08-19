package com.bilibili.cluster.scheduler.common.dto.wx.resp;

import com.bilibili.cluster.scheduler.common.response.BaseResp;
import lombok.Data;

/**
 * @description:
 * @Date: 2024/3/18 19:07
 * @Author: nizhiqiang
 */
@Data
public class NotifyChangeStartResp extends BaseResp {

    private NotifyId data;

    @Data
    public static class NotifyId {
        //        变更id
        Long id;
    }
}
