package com.bilibili.cluster.scheduler.common.dto.flow;

import lombok.Data;

@Data
public class RichedExecutionFlowRuntimeDataDTO<T> extends ExecutionFlowRuntimeDataDTO {

    private T extParams;

}
