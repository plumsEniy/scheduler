package com.bilibili.cluster.scheduler.api.event.clickhouse;

import cn.hutool.json.JSONUtil;
import com.bilibili.cluster.scheduler.api.event.BatchedTaskEventHandler;
import com.bilibili.cluster.scheduler.api.service.bmr.config.BmrConfigService;
import com.bilibili.cluster.scheduler.api.service.caster.CasterService;
import com.bilibili.cluster.scheduler.api.service.clickhouse.clickhouse.ClickhouseService;
import com.bilibili.cluster.scheduler.common.dto.clickhouse.clickhouse.ClickhouseDeployDTO;
import com.bilibili.cluster.scheduler.common.dto.clickhouse.clickhouse.params.CkContainerCapacityFlowExtParams;
import com.bilibili.cluster.scheduler.common.dto.clickhouse.clickhouse.params.CkContainerIterationFlowExtParams;
import com.bilibili.cluster.scheduler.common.dto.flow.ExecutionFlowProps;
import com.bilibili.cluster.scheduler.common.dto.flow.prop.BaseFlowExtPropDTO;
import com.bilibili.cluster.scheduler.common.entity.ExecutionNodeEntity;
import com.bilibili.cluster.scheduler.common.enums.event.EventTypeEnum;
import com.bilibili.cluster.scheduler.common.enums.flow.FlowDeployType;
import com.bilibili.cluster.scheduler.common.event.TaskEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;

/**
 * @description: ck容器发布
 * @Date: 2025/2/12 11:15
 * @Author: nizhiqiang
 */

@Slf4j
@Component
public class CKContainerDeployEventHandler extends BatchedTaskEventHandler {

    @Resource
    ClickhouseService clickhouseService;

    @Resource
    BmrConfigService bmrConfigService;

    @Resource
    CasterService casterService;

    @Override
    public boolean batchExecEvent(TaskEvent taskEvent, List<ExecutionNodeEntity> nodeEntityList) throws Exception {
        logPersist(taskEvent, "ck容器发布任务开始执行");

        ExecutionFlowProps executionFlowProps = taskEvent.getExecutionFlowInstanceDTO().getExecutionFlowProps();
        Long configId = executionFlowProps.getConfigId();
        FlowDeployType deployType = executionFlowProps.getDeployType();
        Long flowId = taskEvent.getFlowId();
        Long componentId = executionFlowProps.getComponentId();

        final BaseFlowExtPropDTO baseFlowExtPropDTO = executionFlowPropsService.getFlowPropByFlowId(flowId, BaseFlowExtPropDTO.class);

        String flowExtParamStr = baseFlowExtPropDTO.getFlowExtParams();
        ClickhouseDeployDTO clickhouseDeployDTO;
        String podTemplate;
        switch (deployType) {
            case K8S_CAPACITY_EXPANSION:
                CkContainerCapacityFlowExtParams ckContainerCapacityFlowExtParams = JSONUtil.toBean(flowExtParamStr, CkContainerCapacityFlowExtParams.class);
                List<Integer> shardAllocationList = ckContainerCapacityFlowExtParams.getShardAllocationList();
                podTemplate = ckContainerCapacityFlowExtParams.getPodTemplate();
                clickhouseDeployDTO = clickhouseService.buildScaleDeployDTO(configId, podTemplate, shardAllocationList);
                break;
            case K8S_ITERATION_RELEASE:
                CkContainerIterationFlowExtParams ckContainerIterationFlowExtParams = JSONUtil.toBean(flowExtParamStr, CkContainerIterationFlowExtParams.class);
                podTemplate = ckContainerIterationFlowExtParams.getPodTemplate();
                List<String> iterationPodList = ckContainerIterationFlowExtParams.getIterationPodList();
                clickhouseDeployDTO = clickhouseService.buildIterationDeployDTO(configId, podTemplate, iterationPodList);
                break;
            default:
                throw new IllegalArgumentException("不支持的部署类型" + deployType);
        }

        clickhouseService.updateShardFile(componentId, clickhouseDeployDTO.getChConfig().getClusters());

        logPersist(taskEvent, "ck发布请求为:\n" + JSONUtil.toJsonStr(clickhouseDeployDTO));
        casterService.deployClickHouse(clickhouseDeployDTO);
        logPersist(taskEvent, "请求发送成功");
        return true;
    }

    @Override
    public int getMinLoopWait() {
        return 3_000;
    }

    @Override
    public int getMaxLoopStep() {
        return 100_000;
    }

    @Override
    public void printLog(TaskEvent taskEvent, String logContent) {

    }

    @Override
    public int logMod() {
        return 10;
    }

    @Override
    public EventTypeEnum getHandleEventType() {
        return EventTypeEnum.CK_CONTAINER_DEPLOY;
    }
}
