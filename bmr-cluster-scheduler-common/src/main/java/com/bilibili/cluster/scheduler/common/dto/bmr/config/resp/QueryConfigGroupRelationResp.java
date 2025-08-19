package com.bilibili.cluster.scheduler.common.dto.bmr.config.resp;

import com.bilibili.cluster.scheduler.common.dto.bmr.config.model.ConfigGroupRelationEntity;
import com.bilibili.cluster.scheduler.common.response.BaseMsgResp;
import lombok.Data;

import java.util.List;

/**
 * @description: 查询配置信息
 * @Date: 2024/6/13 17:00
 * @Author: nizhiqiang
 */

@Data
public class QueryConfigGroupRelationResp extends BaseMsgResp {

    private List<ConfigGroupRelationEntity> obj;
}
