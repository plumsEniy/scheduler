package com.bilibili.cluster.scheduler.common.dto.flow.req;

import lombok.Data;

import javax.validation.constraints.NotEmpty;
import java.util.List;

@Data
public class QueryFlowListReq {

    @NotEmpty(message = "flow id list is empty")
    private List<Long> flowIdList;

}
