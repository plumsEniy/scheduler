package com.bilibili.cluster.scheduler.common.dto.scheduler.req;

import com.bilibili.cluster.scheduler.common.enums.scheduler.ExecuteType;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * @description:
 * @Date: 2024/5/13 20:34
 * @Author: nizhiqiang
 */

@Data
@AllArgsConstructor
public class OperateSchedInstanceReq {
    private int processInstanceId;
    private ExecuteType executeType;
}
