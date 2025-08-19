package com.bilibili.cluster.scheduler.common.dto.scheduler.resp;

import com.bilibili.cluster.scheduler.common.dto.scheduler.model.SchedInstanceData;
import lombok.Data;

/**
 * @description: resp
 * @Date: 2024/5/13 19:21
 * @Author: nizhiqiang
 */

@Data
public class SchedInstanceResp extends BaseDolphinSchedulerResp{
    private SchedInstanceData data;

}
