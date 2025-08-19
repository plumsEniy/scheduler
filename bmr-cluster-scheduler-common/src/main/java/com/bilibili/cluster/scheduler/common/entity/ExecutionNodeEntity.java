package com.bilibili.cluster.scheduler.common.entity;

import com.alibaba.fastjson.annotation.JSONField;
import com.baomidou.mybatisplus.annotation.*;
import com.bilibili.cluster.scheduler.common.Constants;
import com.bilibili.cluster.scheduler.common.enums.NodeExecuteStatusEnum;
import com.bilibili.cluster.scheduler.common.enums.node.NodeExecType;
import com.bilibili.cluster.scheduler.common.enums.node.NodeOperationResult;
import com.bilibili.cluster.scheduler.common.enums.node.NodeType;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 如果需要insert的时候忽略null的属性，需要在字段上加上    @TableField(fill = FieldFill.UPDATE)注解
 * </p>
 *
 * @author 谢谢谅解
 * @since 2024-01-19
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("scheduler_execution_node_v2")
@ApiModel(value = "ExecutionNode对象", description = "")
public class ExecutionNodeEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "自增id")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @ApiModelProperty(value = "节点名")
    private String nodeName;

    @ApiModelProperty(value = "flow id")
    private Long flowId;

    @ApiModelProperty(value = "任务批次id")
    private Integer batchId;

    @ApiModelProperty(value = "额外参数id")
    @TableField(fill = FieldFill.UPDATE)
    private Long extraPropsId;

    @ApiModelProperty(value = "操作人")
    private String operator;

    @ApiModelProperty(value = "任务状态")
    private NodeExecuteStatusEnum nodeStatus;

    @ApiModelProperty(value = "删除标识")
    @TableField(fill = FieldFill.UPDATE)
    private Boolean deleted;

    @ApiModelProperty(value = "创建时间")
    @TableField(fill = FieldFill.UPDATE)
    @JSONField(format = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime ctime;

    @ApiModelProperty(value = "更新时间")
    @TableField(fill = FieldFill.UPDATE)
    @JSONField(format = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime mtime;

    @ApiModelProperty(value = "开始时间")
    @TableField(fill = FieldFill.UPDATE)
    @JSONField(format = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    @ApiModelProperty(value = "结束时间")
    @TableField(fill = FieldFill.UPDATE)
    @JSONField(format = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;

    @ApiModelProperty(value = "任务的操作结果")
    private NodeOperationResult operationResult;

    @ApiModelProperty(value = "机架信息")
    private String rack = Constants.EMPTY_STRING;

    @ApiModelProperty(value = "ip信息")
    private String ip = Constants.EMPTY_STRING;

    @ApiModelProperty(value = "执行所在阶段")
    private String execStage = Constants.EMPTY_STRING;

    @ApiModelProperty(value = "节点执行所在的批次实例Id")
    @TableField(fill = FieldFill.UPDATE)
    private Long instanceId = 0l;

    private String podName;

    private String podStatus;

    @ApiModelProperty(value = "节点类型")
    private NodeType nodeType = NodeType.NORMAL;

    @ApiModelProperty(value = "节点执行态")
    @TableField(fill = FieldFill.UPDATE)
    private NodeExecType execType;

    @ApiModelProperty(value = "执行所在节点")
    @TableField(fill = FieldFill.UPDATE)
    private String execHost;

}
