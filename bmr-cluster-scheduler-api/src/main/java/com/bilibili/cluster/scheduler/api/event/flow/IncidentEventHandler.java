package com.bilibili.cluster.scheduler.api.event.flow;

import com.bilibili.cluster.scheduler.api.service.incident.IncidentTransferService;
import com.bilibili.cluster.scheduler.common.entity.ExecutionFlowEntity;
import com.bilibili.cluster.scheduler.common.enums.flow.FlowAopEventType;
import com.bilibili.cluster.scheduler.common.enums.flow.FlowStatusEnum;
import com.bilibili.cluster.scheduler.common.enums.incident.IncidentStatus;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * @description:
 * @Date: 2025/5/12 17:09
 * @Author: nizhiqiang
 */

@Component
public class IncidentEventHandler implements AbstractFlowAopEventHandler {

    @Resource
    IncidentTransferService incidentTransferService;

    @Override
    public boolean createFlow(ExecutionFlowEntity flow) {
        incidentTransferService.startIncident(flow);
        return true;
    }

    @Override
    public boolean giveUpFlow(ExecutionFlowEntity flow) {
        incidentTransferService.updateIncidentStatus(flow, IncidentStatus.GIVE_UP);
        return true;
    }

    @Override
    public boolean finishFlow(ExecutionFlowEntity flow, FlowStatusEnum beforeStatus) {
        IncidentStatus incidentStatus;
        switch (beforeStatus) {
            case SUCCEED_EXECUTE:
                incidentStatus = IncidentStatus.FINISH;
                break;
            case ROLLBACK_SUCCESS:
                incidentStatus = IncidentStatus.ROLLBACK;
                break;
            default:
                incidentStatus = IncidentStatus.FAIL;
        }

        incidentTransferService.updateIncidentStatus(flow, incidentStatus);
        return true;
    }

    @Override
    public FlowAopEventType getEventType() {
        return FlowAopEventType.INCIDENT;
    }
}
