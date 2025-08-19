package com.bilibili.cluster.scheduler.common.dto.node.req;

import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Positive;
import java.util.List;

/**
 * @description: 批量重试任务
 * @Date: 2024/2/22 10:45
 * @Author: nizhiqiang
 */
@Data
public class BatchRetryNodeReq {

    @Positive(message = "工作流id非法")
    private Long flowId;

    // @NotEmpty(message = "任务名列表不能为空")
    private List<String> nodeNameList;

    private List<Long> nodeIdList;
}
