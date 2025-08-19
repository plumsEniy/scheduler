package com.bilibili.cluster.scheduler.api.event.flow;

import com.bilibili.cluster.scheduler.api.service.flow.ExecutionFlowService;
import com.bilibili.cluster.scheduler.api.service.wx.WxPublisherService;
import com.bilibili.cluster.scheduler.common.Constants;
import com.bilibili.cluster.scheduler.common.entity.ExecutionFlowEntity;
import com.bilibili.cluster.scheduler.common.entity.ExecutionNodeEntity;
import com.bilibili.cluster.scheduler.common.enums.flow.FlowAopEventType;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * @description:
 * @Date: 2025/5/12 17:12
 * @Author: nizhiqiang
 */

@Component
public class JobFailNotifyEventHandler implements AbstractFlowAopEventHandler {

    @Resource
    WxPublisherService wxPublisherService;

    @Resource
    ExecutionFlowService executionFlowService;

    @Override
    public boolean jobFail(ExecutionFlowEntity flow, ExecutionNodeEntity executionNode, String errorMsg) {
        String operator = executionNode.getOperator();
        List<String> operatorList = new ArrayList<>();
        operatorList.addAll(executionFlowService.getOpAdminList());
        if (!operatorList.contains(operator)) {
            if (!operator.toLowerCase(Locale.ROOT).contains("bmr")) {
                operatorList.add(operator);
            }
        }
        wxPublisherService.wxPushMsg(operatorList, Constants.MSG_TYPE_TEXT, errorMsg);
        return true;
    }

    @Override
    public FlowAopEventType getEventType() {
        return FlowAopEventType.JOB_FAIL_NOTIFY;
    }
}
