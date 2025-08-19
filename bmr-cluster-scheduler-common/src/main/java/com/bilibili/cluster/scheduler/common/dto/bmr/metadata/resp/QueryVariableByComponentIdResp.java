package com.bilibili.cluster.scheduler.common.dto.bmr.metadata.resp;

import com.bilibili.cluster.scheduler.common.dto.bmr.metadata.model.ComponentVariable;
import com.bilibili.cluster.scheduler.common.response.BaseMsgResp;
import lombok.Data;

import java.util.List;

/**
 * @description: 查询组件变量
 * @Date: 2024/5/15 15:20
 * @Author: nizhiqiang
 */

@Data
public class QueryVariableByComponentIdResp extends BaseMsgResp {
    private List<ComponentVariable> obj;


}
