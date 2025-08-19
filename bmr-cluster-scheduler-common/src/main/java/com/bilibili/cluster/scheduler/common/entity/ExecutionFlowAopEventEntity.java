package com.bilibili.cluster.scheduler.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.bilibili.cluster.scheduler.common.enums.flow.FlowAopEventType;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 工作流切片事件
 * </p>
 *
 * @author 谢谢谅解
 * @since 2025-05-12
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("scheduler_execution_flow_aop_event")
@ApiModel(value="ExecutionFlowAopEvent对象", description="工作流切片事件")
public class ExecutionFlowAopEventEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "自增id")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @ApiModelProperty(value = "flow id")
    private Long flowId;

    @ApiModelProperty(value = "触发次数")
    private Integer count;

    @ApiModelProperty(value = "工作流事件类型")
    private FlowAopEventType eventType;

    @ApiModelProperty(value = "创建时间")
    private LocalDateTime ctime;

    @ApiModelProperty(value = "更新时间")
    private LocalDateTime mtime;

    private String props;

    @TableLogic
    @ApiModelProperty(value = "删除标识")
    private Boolean deleted;


}
