package com.bilibili.cluster.scheduler.api.service.incident;

import com.bilibili.cluster.scheduler.common.entity.ExecutionFlowEntity;
import com.bilibili.cluster.scheduler.common.enums.incident.IncidentStatus;

public interface IncidentTransferService {
    /**
     * 开始变更
     * @param flow
     */
    void startIncident(ExecutionFlowEntity flow);

    /**
     * 更新变更
     * @param flow
     * @param status
     */
    void updateIncidentStatus(ExecutionFlowEntity flow, IncidentStatus status);

}
