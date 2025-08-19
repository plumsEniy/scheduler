package com.bilibili.cluster.scheduler.api.event.hdfs.nnproxy.restart;

import cn.hutool.json.JSONUtil;
import com.bilibili.cluster.scheduler.api.event.dolphinScheduler.AbstractDolphinSchedulerEventHandler;
import com.bilibili.cluster.scheduler.common.Constants;
import com.bilibili.cluster.scheduler.common.entity.ExecutionFlowEntity;
import com.bilibili.cluster.scheduler.common.enums.event.EventTypeEnum;
import com.bilibili.cluster.scheduler.common.event.TaskEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

@Slf4j
@Component
public class NNProxyRestartPipelineEventHandler extends AbstractDolphinSchedulerEventHandler {

    @Override
    public EventTypeEnum getHandleEventType() {
        return EventTypeEnum.NN_PROXY_RESTART_PIPELINE_EVENT;
    }

    @Override
    protected Map<String, Object> getDolphinExecuteEnv(TaskEvent taskEvent, List<String> hostList) {
        ExecutionFlowEntity flowEntity = executionFlowService.getById(taskEvent.getFlowId());

        Map<String, Object> instanceEnv = new LinkedHashMap<>();
        instanceEnv.put(Constants.COMPONENT_ROLE, flowEntity.getRoleName());
        instanceEnv.put(Constants.COMPONENT_CLUSTER, flowEntity.getClusterName());
        instanceEnv.put(Constants.FLOW_ID, flowEntity.getId());
        instanceEnv.put(Constants._JOB_EXCUTE_TYPE, flowEntity.getJobExecuteType());
        instanceEnv.put(Constants.RELASE_SCOPE, flowEntity.getReleaseScopeType());

        log.info("JOB_EXECUTE_TYPE :" + flowEntity.getJobExecuteType());
        instanceEnv.put(Constants.DEPLOYMENT_ORDER_CREATOR, flowEntity.getOperator());

        StringJoiner joiner = new StringJoiner(Constants.COMMA);
        hostList.forEach(joiner::add);
        // 机器列表
        instanceEnv.put(Constants.SYSTEM_JOBAGENT_EXEC_HOSTS, joiner.toString());
        // 服务是否重启
        instanceEnv.put(Constants.SERVICE_RESTART, flowEntity.getRestart());
        // 生效方式
        instanceEnv.put(Constants.EFFECTIVE_MODE, flowEntity.getEffectiveMode());


        instanceEnv.put(Constants.CI_PACK_ID, Constants.EMPTY_STRING);
        instanceEnv.put(Constants.CI_PACK_MD5, Constants.EMPTY_STRING);
        instanceEnv.put(Constants.CI_PACK_NAME, Constants.EMPTY_STRING);
        instanceEnv.put(Constants.CI_PACK_TAG_NAME, Constants.EMPTY_STRING);
        instanceEnv.put(Constants.CI_PACK_VERSION, Constants.EMPTY_STRING);
        instanceEnv.put(Constants.CI_PACK_URL, Constants.EMPTY_STRING);
        instanceEnv.put(Constants.HOST_ENV_MAP_KEY, JSONUtil.toJsonStr(Collections.EMPTY_MAP));

        instanceEnv.put(Constants.CONFIG_PACK_ID, Constants.EMPTY_STRING);
        instanceEnv.put(Constants.CONFIG_PACK_MD5, Constants.EMPTY_STRING);
        instanceEnv.put(Constants.CONFIG_PACK_NAME, Constants.EMPTY_STRING);
        instanceEnv.put(Constants.CONFIG_PACK_VERSION, Constants.EMPTY_STRING);
        instanceEnv.put(Constants.CONFIG_PACK_URL, Constants.EMPTY_STRING);

        return instanceEnv;
    }
}
