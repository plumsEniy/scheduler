
package com.bilibili.cluster.scheduler.common.event;


import com.bilibili.cluster.scheduler.common.Constants;
import com.bilibili.cluster.scheduler.common.entity.ExecutionNodeEntity;
import com.bilibili.cluster.scheduler.common.entity.ExecutionNodeEventEntity;
import com.bilibili.cluster.scheduler.common.enums.event.EventReleaseScope;
import com.bilibili.cluster.scheduler.common.enums.event.EventStatusEnum;
import com.bilibili.cluster.scheduler.common.dto.flow.ExecutionFlowInstanceDTO;
import com.bilibili.cluster.scheduler.common.enums.event.EventTypeEnum;
import com.bilibili.cluster.scheduler.common.enums.flow.FlowDeployType;
import com.bilibili.cluster.scheduler.common.enums.node.NodeOperationResult;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Random;

/**
 * task event
 */
@Data
public class TaskEvent {

    /**
     * instanceId
     */
    private Long instanceId;

    /**
     * flowId
     */
    private Long flowId;

    /**
     * batchId
     */
    private Integer batchId;

    /**
     * nodeId
     */
    private Long executionNodeId;

    /**
     * eventId
     */
    private Long eventId;

    /**
     * 事件类型
     */
    private EventTypeEnum eventTypeEnum;

    /**
     * 事件名称
     */
    private String eventName;

    /**
     * flow 类型
     */
    private FlowDeployType deployType;

    /**
     * nodeName
     */
    private String nodeName;

    /**
     * 执行顺序
     */
    private Integer executeOrder;

    /**
     * 事件状态
     */
    private EventStatusEnum eventStatus;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    /**
     * 执行实例主机
     */
    private String hostName;

    private ExecutionFlowInstanceDTO executionFlowInstanceDTO;

    private ExecutionNodeEntity executionNode;

    private ExecutionNodeEventEntity eventEntity;

    private EventReleaseScope releaseScope;

    // 反馈的节点状态
    private NodeOperationResult operationResult;

    // 节点定义
    private String SchedulerTaskCode;
    // 节点名称
    private String taskName;
    // job-agent taskSet id
    private long jobTaskSetId;
    // job-agent task id
    private long jobTaskId;

    // retry prop
    private boolean autoRetry;

    private int maxAttemptNumber;

    private Random random = new Random();

    public String getSummary() {
        StringBuilder builder = new StringBuilder();
        builder.append("flowId=").append(flowId).append(Constants.COMMA)
                .append("batchId=").append(batchId).append(Constants.COMMA)
                .append("instanceId=").append(instanceId).append(Constants.COMMA)
                .append("nodeName=").append(nodeName).append(Constants.COMMA)
                .append("eventName=").append(eventName).append(Constants.COMMA)
                .append("executeOrder=").append(executeOrder).append(Constants.POINT);
        return builder.toString();
    }
}
