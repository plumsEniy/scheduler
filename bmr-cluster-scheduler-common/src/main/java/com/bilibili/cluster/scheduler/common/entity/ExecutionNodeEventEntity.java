package com.bilibili.cluster.scheduler.common.entity;

import com.alibaba.fastjson.annotation.JSONField;
import com.baomidou.mybatisplus.annotation.*;

import java.time.LocalDateTime;
import java.io.Serializable;
import java.util.Objects;

import com.bilibili.cluster.scheduler.common.enums.event.EventStatusEnum;
import com.bilibili.cluster.scheduler.common.enums.event.EventTypeEnum;
import com.bilibili.cluster.scheduler.common.enums.event.EventReleaseScope;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 *
 * @author 谢谢谅解
 * @since 2024-01-19
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("scheduler_execution_node_event")
@ApiModel(value = "NodeEvent对象", description = "")
public class ExecutionNodeEventEntity implements Serializable, Comparable<ExecutionNodeEventEntity> {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "自增id")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @ApiModelProperty(value = "事件类型")
    private EventTypeEnum eventType;

    @ApiModelProperty(value = "事件名称")
    private String eventName;

    @ApiModelProperty(value = "ExecutionNodeEntity 表ID")
    private Long executionNodeId;

    @ApiModelProperty(value = "flow id")
    private Long flowId;

    @ApiModelProperty(value = "批次id")
    private Integer batchId;

    @ApiModelProperty(value = "状态")
    private EventStatusEnum eventStatus;

    @ApiModelProperty(value = "范围状态")
    private EventReleaseScope releaseScope;

    @ApiModelProperty(value = "执行实例主机名")
    private String hostName;

    @ApiModelProperty(value = "执行顺序")
    private Integer executeOrder;

    @ApiModelProperty(value = "日志id")
    @TableField(fill = FieldFill.UPDATE)
    private Long logId;

    @ApiModelProperty(value = "dolphin的id")
    private String schedInstanceId;

    @ApiModelProperty(value = "开始时间")
    @TableField(fill = FieldFill.UPDATE)
    @JSONField(format = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    @ApiModelProperty(value = "结束时间")
    @TableField(fill = FieldFill.UPDATE)
    @JSONField(format = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;

    @ApiModelProperty(value = "创建时间")
    @TableField(fill = FieldFill.UPDATE)
    @JSONField(format = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime ctime;

    @ApiModelProperty(value = "更新时间")
    @TableField(fill = FieldFill.UPDATE)
    @JSONField(format = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime mtime;

    @ApiModelProperty(value = "删除标识")
    @TableField(fill = FieldFill.UPDATE)
    private Boolean deleted;

    @ApiModelProperty(value = "dolphin项目code")
    private String projectCode;

    @ApiModelProperty(value = "dolphin流程code")
    private String pipelineCode;

    @ApiModelProperty(value = "dolphin任务code")
    private String taskCode;

    @ApiModelProperty(value = "dolphin任务失败策略")
    private String failureStrategy;

    @ApiModelProperty(value = "dolphin节点类型")
    private String taskPosType;

    @ApiModelProperty(value = "jobAgent结果集Id")
    @TableField(fill = FieldFill.UPDATE)
    private Long jobTaskSetId;

    @ApiModelProperty(value = "jobAgent单任务Id")
    @TableField(fill = FieldFill.UPDATE)
    private Long jobTaskId;

    @ApiModelProperty(value = "执行实例Id")
    @TableField(fill = FieldFill.UPDATE)
    private Long instanceId;


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ExecutionNodeEventEntity that = (ExecutionNodeEventEntity) o;
        return id.equals(that.id) && executeOrder.equals(that.executeOrder) && eventType == that.eventType && Objects.equals(flowId, that.flowId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, eventType, flowId, executeOrder);
    }

    @Override
    public int compareTo(ExecutionNodeEventEntity o) {
        return this.getExecuteOrder().compareTo(o.getExecuteOrder());
    }
}
