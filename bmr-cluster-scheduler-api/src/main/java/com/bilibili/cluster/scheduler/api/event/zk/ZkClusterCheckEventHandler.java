package com.bilibili.cluster.scheduler.api.event.zk;

import com.bilibili.cluster.scheduler.api.event.AbstractTaskEventHandler;
import com.bilibili.cluster.scheduler.api.service.bmr.resource.BmrResourceService;
import com.bilibili.cluster.scheduler.api.service.flow.ExecutionNodeService;
import com.bilibili.cluster.scheduler.common.Constants;
import com.bilibili.cluster.scheduler.common.dto.flow.ExecutionFlowProps;
import com.bilibili.cluster.scheduler.common.entity.ExecutionNodeEntity;
import com.bilibili.cluster.scheduler.common.enums.event.EventTypeEnum;
import com.bilibili.cluster.scheduler.common.enums.flow.FlowDeployType;
import com.bilibili.cluster.scheduler.common.enums.node.NodeType;
import com.bilibili.cluster.scheduler.common.event.TaskEvent;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @description: zk集群检测
 * @Date: 2025/7/10 14:41
 * @Author: nizhiqiang
 */

@Component
public class ZkClusterCheckEventHandler extends AbstractTaskEventHandler {

    @Resource
    BmrResourceService bmrResourceService;

    @Resource
    ExecutionNodeService executionNodeService;

    @Override
    public boolean executeTaskEvent(TaskEvent taskEvent) throws Exception {

        List<ExecutionNodeEntity> executionNodeList = executionNodeService.queryAllExecutionNodeByFlowId(taskEvent.getFlowId());
        List<String> executionNodeNameList = executionNodeList.stream()
                .filter(node -> node.getNodeType().isNormalNode())
                .map(ExecutionNodeEntity::getNodeName)
                .collect(Collectors.toList());

        ExecutionFlowProps executionFlowProps = taskEvent.getExecutionFlowInstanceDTO().getExecutionFlowProps();
        Long clusterId = executionFlowProps.getClusterId();
        Long componentId = executionFlowProps.getComponentId();
        FlowDeployType deployType = executionFlowProps.getDeployType();
        if (FlowDeployType.OFF_LINE_EVICTION.equals(deployType) && CollectionUtils.isEmpty(executionNodeNameList)) {
            logPersist(taskEvent, "下线且集群列表为空不检测角色状态");
            return true;
        }
        logPersist(taskEvent, "等待2分钟后检测");
        Thread.sleep(Constants.ONE_MINUTES * 2);

        LocalDateTime startTime = LocalDateTime.now();
        while (Duration.between(startTime, LocalDateTime.now()).toMinutes() < 10) {


            List<String> leaderHostNameList = executionNodeNameList.stream()
                    .filter(nodeName -> Constants.ZK_LEADER.equals(bmrResourceService.queryZkRole(nodeName)))
                    .collect(Collectors.toList());

            if (leaderHostNameList.size() == 1) {
                logPersist(taskEvent, String.format("集群正常,当前leader节点为:%s", leaderHostNameList.get(0)));
                return true;
            }

            logPersist(taskEvent, String.format("集群异常,当前leader节点为:%s", leaderHostNameList));
            logPersist(taskEvent, "等待1分钟");
            Thread.sleep(Constants.ONE_MINUTES * 1);
        }
        logPersist(taskEvent, "集群检测超时");

        return false;
    }

    @Override
    protected boolean checkEventIsRequired(TaskEvent taskEvent) {
        final ExecutionNodeEntity executionNode = taskEvent.getExecutionNode();
        final String execStage = executionNode.getExecStage();
        NodeType nodeType = executionNode.getNodeType();
        if (nodeType.isNormalNode()) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public EventTypeEnum getHandleEventType() {
        return EventTypeEnum.ZK_CLUSTER_STATUS_CHECK;
    }
}
