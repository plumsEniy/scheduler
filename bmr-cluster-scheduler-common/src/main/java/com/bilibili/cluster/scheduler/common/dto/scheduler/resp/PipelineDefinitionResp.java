package com.bilibili.cluster.scheduler.common.dto.scheduler.resp;

import com.bilibili.cluster.scheduler.common.dto.scheduler.model.DagData;
import lombok.Data;

/**
 * @description: 查询定义
 * @Date: 2024/5/13 19:43
 * @Author: nizhiqiang
 */
@Data
public class PipelineDefinitionResp extends BaseDolphinSchedulerResp{
    private DagData data;

}
