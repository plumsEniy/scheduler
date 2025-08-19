package com.bilibili.cluster.scheduler.common.dto.node;

import com.bilibili.cluster.scheduler.common.entity.ExecutionFlowEntity;
import com.bilibili.cluster.scheduler.common.entity.ExecutionNodeEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BatchNodeExecDTO {

    private ExecutionFlowEntity flowEntity;

    /**
     * 待执行的节点列表
     */
    private List<ExecutionNodeEntity> targetExecutionNodeList;

}
