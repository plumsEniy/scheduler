package com.bilibili.cluster.scheduler.common.dto.node;

import com.bilibili.cluster.scheduler.common.entity.ExecutionNodeEntity;
import com.bilibili.cluster.scheduler.common.entity.ExecutionNodeEventEntity;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * @description: 查询详情
 * @Date: 2024/2/1 14:18
 * @Author: nizhiqiang
 */
@Data
public class BaseExecutionNodeDTO extends ExecutionNodeEntity {

    private List<ExecutionNodeEventEntity> executionNodeEventEntityList = new ArrayList<>();
}
