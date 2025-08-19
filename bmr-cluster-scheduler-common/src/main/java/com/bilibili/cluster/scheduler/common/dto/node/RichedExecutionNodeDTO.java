package com.bilibili.cluster.scheduler.common.dto.node;

import lombok.Data;

@Data
public class RichedExecutionNodeDTO<T> extends BaseExecutionNodeDTO {

    private T nodeProps;

}
