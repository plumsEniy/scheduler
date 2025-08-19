package com.bilibili.cluster.scheduler.common.dto.bmr.config.resp;

import com.bilibili.cluster.scheduler.common.dto.bmr.config.ConfigGroupDto;
import com.bilibili.cluster.scheduler.common.response.BaseMsgResp;
import lombok.Data;

/**
 * @description: 查询配置组信息
 * @Date: 2024/5/15 16:22
 * @Author: nizhiqiang
 */
@Data
public class QueryConfigGroupInfoByIdResp extends BaseMsgResp {
    private ConfigGroupDto obj;

}
