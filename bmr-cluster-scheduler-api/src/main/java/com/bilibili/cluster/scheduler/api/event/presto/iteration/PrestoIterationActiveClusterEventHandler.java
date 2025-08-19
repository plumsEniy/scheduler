package com.bilibili.cluster.scheduler.api.event.presto.iteration;


import cn.hutool.core.thread.ThreadUtil;
import com.bilibili.cluster.scheduler.api.event.AbstractBranchedTaskEventHandler;
import com.bilibili.cluster.scheduler.api.service.bmr.metadata.BmrMetadataService;
import com.bilibili.cluster.scheduler.api.service.presto.PrestoService;
import com.bilibili.cluster.scheduler.common.Constants;
import com.bilibili.cluster.scheduler.common.dto.bmr.metadata.MetadataClusterData;
import com.bilibili.cluster.scheduler.common.dto.bmr.metadata.enums.ClusterNetworkEnvironmentEnum;
import com.bilibili.cluster.scheduler.common.dto.presto.PrestoYamlObj;
import com.bilibili.cluster.scheduler.common.dto.presto.iteration.PrestoIterationExtNodeParams;
import com.bilibili.cluster.scheduler.common.entity.ExecutionNodeEntity;
import com.bilibili.cluster.scheduler.common.enums.event.EventTypeEnum;
import com.bilibili.cluster.scheduler.common.enums.node.NodeType;
import com.bilibili.cluster.scheduler.common.event.TaskEvent;
import com.google.common.base.Preconditions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class PrestoIterationActiveClusterEventHandler extends AbstractBranchedTaskEventHandler {

    @Resource
    PrestoService prestoService;

    @Resource
    BmrMetadataService metadataService;

    public boolean skipLogicalNode() {
        return false;
    }

    @Override
    public boolean checkEventIsRequired(TaskEvent taskEvent) {
        final ExecutionNodeEntity executionNode = taskEvent.getExecutionNode();
        final String execStage = executionNode.getExecStage();
        if (execStage.equalsIgnoreCase("1")) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * 处理普通节点，不存在回滚分支的场景
     * @param taskEvent
     * @return
     */
    @Override
    protected boolean executeNormalNodeTaskEvent(TaskEvent taskEvent) throws Exception {
        final Long executionNodeId = taskEvent.getExecutionNodeId();
        PrestoIterationExtNodeParams nodeParams = executionNodePropsService.queryNodePropsByNodeId(executionNodeId, PrestoIterationExtNodeParams.class);
        Preconditions.checkNotNull(nodeParams, "nodeParams is null");

        long clusterId = nodeParams.getClusterId();
        MetadataClusterData clusterData = metadataService.queryClusterDetail(clusterId);

        PrestoYamlObj prestoYamlObj = prestoService.buildPrestoYamlObj(nodeParams.getConfigId(), "test");
        String start = prestoYamlObj.getStart();
        if (start.contains(Constants.SLEEP_SH)) {
            return true;
        }

        String clusterName = clusterData.getClusterName();
        ClusterNetworkEnvironmentEnum networkEnvironment = clusterData.getNetworkEnvironment();
        prestoService.activeCluster(clusterName, networkEnvironment.getEnv());
        logPersist(taskEvent, "activate cluster success");
        return true;
    }

    /**
     * 处理逻辑节点，动态调整工作流容错度
     *
     * @param taskEvent
     * @return
     * @throws Exception
     */
    @Override
    protected boolean executeLogicalNodeTaskEvent(TaskEvent taskEvent) throws Exception {
        // 修改工作流容错度
        final ExecutionNodeEntity executionNode = taskEvent.getExecutionNode();
        final NodeType nodeType = executionNode.getNodeType();
        long flowId = taskEvent.getFlowId();

        switch (nodeType) {
            case STAGE_START_NODE:
                // 开始节点，容错度降低为0
                executionFlowService.updateFlowTolerance(flowId, 0);
                logPersist(taskEvent, "start logical node, updateFlowTolerance to 0 success");
                break;
            case STAGE_END_NODE:
                // 结束节点，容错度放到最大
                ExecutionNodeEntity queryDo = new ExecutionNodeEntity();
                queryDo.setFlowId(flowId);
                queryDo.setExecStage("2");
                final List<ExecutionNodeEntity> nodeEntityList = executionNodeService.queryNodeList(queryDo, true);
                int tolerance = nodeEntityList.size();
                executionFlowService.updateFlowTolerance(flowId, tolerance);
                logPersist(taskEvent, "end logical node, updateFlowTolerance to " + tolerance + " success");
                // 等待10分钟，集群初始化启动完成
                logPersist(taskEvent, "sleep 10 minutes.... waiting cluster available");
                ThreadUtil.sleep(10, TimeUnit.MINUTES);
                break;
            default:
                logPersist(taskEvent, "error node type, require logical node, but find " + nodeType);
                return false;
        }
        return true;
    }

    @Override
    public EventTypeEnum getHandleEventType() {
        return EventTypeEnum.PRESTO_ITERATION_ACTIVE_CLUSTER;
    }

}

