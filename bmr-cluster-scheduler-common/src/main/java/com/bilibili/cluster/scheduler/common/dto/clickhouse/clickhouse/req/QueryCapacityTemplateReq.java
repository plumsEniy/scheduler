package com.bilibili.cluster.scheduler.common.dto.clickhouse.clickhouse.req;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Positive;
import java.util.List;

/**
 * @description: 查询扩容模版
 * @Date: 2025/2/10 15:20
 * @Author: nizhiqiang
 */

@Data
public class QueryCapacityTemplateReq {

    @Positive(message = "configVersionId必须大于0")
    private long configVersionId;

    @NotBlank(message = "模版名不能为空")
    private String podTemplate;

    @NotEmpty(message = "shard allocation列表不能为空")
    private List<Integer> shardAllocationList;
}
