package com.bilibili.cluster.scheduler.api.event.dolphinScheduler.env;

import com.bilibili.cluster.scheduler.api.service.GlobalService;
import com.bilibili.cluster.scheduler.api.service.flow.ExecutionFlowService;
import com.bilibili.cluster.scheduler.api.service.flow.ExecutionNodeEventService;
import com.bilibili.cluster.scheduler.api.service.flow.ExecutionNodeService;
import com.bilibili.cluster.scheduler.common.event.TaskEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Map;

@Slf4j
@Component
public class DolphinEnvExtendedServiceImpl implements DolphinEnvExtendedService {

    @Resource
    ExecutionFlowService flowService;

    @Resource
    ExecutionNodeService nodeService;

    @Resource
    ExecutionNodeEventService nodeEventService;

    @Resource
    GlobalService globalService;

    @Override
    public void fillExtendedEnv(TaskEvent taskEvent, Map<String, Object> env) {
        // 按需填充

    }
}
