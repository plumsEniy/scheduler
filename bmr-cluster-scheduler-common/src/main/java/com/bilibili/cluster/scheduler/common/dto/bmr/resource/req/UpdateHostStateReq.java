package com.bilibili.cluster.scheduler.common.dto.bmr.resource.req;

import com.bilibili.cluster.scheduler.common.enums.flow.FlowDeployType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @description: 更新主机状态
 * @Date: 2024/5/28 14:50
 * @Author: nizhiqiang
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdateHostStateReq {
    private long clusterId;
    private long componentId;
    private List<String> hostList;
    private FlowDeployType deployTypeEnum;
    private boolean success;

    private String packageDiskVersion;
    private String configDiskVersion;
}
