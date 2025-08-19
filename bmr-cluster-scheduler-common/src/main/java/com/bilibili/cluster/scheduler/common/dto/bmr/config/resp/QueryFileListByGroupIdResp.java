package com.bilibili.cluster.scheduler.common.dto.bmr.config.resp;

import com.bilibili.cluster.scheduler.common.dto.bmr.config.model.ConfigFileEntity;
import com.bilibili.cluster.scheduler.common.response.BaseMsgResp;
import lombok.Data;

import java.util.List;

/**
 * @description: 查询文件id
 * @Date: 2024/6/13 17:18
 * @Author: nizhiqiang
 */

@Data
public class QueryFileListByGroupIdResp extends BaseMsgResp {

    private List<ConfigFileEntity> obj;
}
