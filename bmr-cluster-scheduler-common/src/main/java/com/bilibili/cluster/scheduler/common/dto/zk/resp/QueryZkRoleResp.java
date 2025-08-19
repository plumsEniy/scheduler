package com.bilibili.cluster.scheduler.common.dto.zk.resp;

import com.bilibili.cluster.scheduler.common.dto.yarn.RMInfoObj;
import com.bilibili.cluster.scheduler.common.response.BaseMsgResp;
import lombok.Data;

/**
 * @description:
 * @Date: 2025/7/14 17:34
 * @Author: nizhiqiang
 */

@Data
public class QueryZkRoleResp extends BaseMsgResp {

    private String obj;
}
