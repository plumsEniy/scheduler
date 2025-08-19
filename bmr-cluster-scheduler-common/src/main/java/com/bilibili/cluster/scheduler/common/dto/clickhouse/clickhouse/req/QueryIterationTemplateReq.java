package com.bilibili.cluster.scheduler.common.dto.clickhouse.clickhouse.req;

import com.bilibili.cluster.scheduler.common.enums.flow.FlowDeployType;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import java.util.List;

/**
 * @description: 查询扩容模版
 * @Date: 2025/2/10 15:20
 * @Author: nizhiqiang
 */

@Data
public class QueryIterationTemplateReq {

    @Positive(message = "configId必须大于0")
    private long configId;

    @Positive(message = "packageId必须大于0")
    private long packageId;

    @Positive(message = "componentId必须大于0")
    private long componentId;

    @Positive(message = "clusterId必须大于0")
    private long clusterId;

    @NotBlank(message = "模版名不能为空")
    private String podTemplate;

    @NotNull(message = "发布类型不能为空")
    private FlowDeployType deployType;

    /**
     * 扩容的shard列表
     */
    private List<Integer> shardAllocationList;

    /**
     * 迭代的节点列表
     */
    private List<String> nodeList;
}
