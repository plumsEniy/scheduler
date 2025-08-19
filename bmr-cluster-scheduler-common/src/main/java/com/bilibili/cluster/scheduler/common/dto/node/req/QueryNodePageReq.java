package com.bilibili.cluster.scheduler.common.dto.node.req;

import com.bilibili.cluster.scheduler.common.enums.NodeExecuteStatusEnum;
import lombok.Data;

import javax.validation.constraints.Positive;

/**
 * @description: 分页查询job
 * @Date: 2024/1/30 20:18
 * @Author: nizhiqiang
 */

@Data
public class QueryNodePageReq {

    @Positive(message = "flowid不合法")
    private long flowId;
    /**
     * 模糊查询
     */
    private String nodeName;

    /**
     * 发布阶段
     */
    private String execStage;

    private NodeExecuteStatusEnum nodeStatus;

    @Positive(message = "page num is illegal")
    private int pageNum = 1;

    @Positive(message = "page size is illegal")
    private int pageSize = 10;

    private boolean skipLogicalNode;

    private String nodeType;
}
