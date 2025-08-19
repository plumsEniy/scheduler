package com.bilibili.cluster.scheduler.common.dto.node.req;

import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Positive;
import java.util.List;

/**
 * @description: 批量跳过任务
 * @Date: 2024/3/18 16:53
 * @Author: nizhiqiang
 */
@Data
public class BatchSkipNodeReq {
    @Positive(message = "工作流id非法")
    private Long flowId;

    @NotEmpty(message = "任务名列表不能为空")
    private List<String> nodeNameList;
}
