package com.bilibili.cluster.scheduler.api.service.flow.check;

import com.bilibili.cluster.scheduler.common.dto.flow.req.DeployOneFlowReq;

public interface FlowParamsValidator {

    /**
     * 检查 && 填充基础参数信息
     * @param req
     */
    void validate(DeployOneFlowReq req) throws Exception;

}
