package com.bilibili.cluster.scheduler.common.entity;

import com.alibaba.fastjson.annotation.JSONField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;

import java.time.LocalDateTime;
import java.io.Serializable;

import com.bilibili.cluster.scheduler.common.enums.flowLog.LogTypeEnum;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 *
 * </p>
 *
 * @author 谢谢谅解
 * @since 2024-01-19
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("scheduler_execution_log")
@ApiModel(value = "ExecutionLog对象", description = "")
public class ExecutionLogEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "自增id")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @ApiModelProperty(value = "枚举类型字段：执行侧的日志类型")
    private LogTypeEnum logType;

    @ApiModelProperty(value = "对应flowId 或者 eventId")
    private Long executeId;

    @ApiModelProperty(value = "日志内容")
    private String logContent;

    @ApiModelProperty(value = "删除标识")
    private Boolean deleted;

    @ApiModelProperty(value = "创建时间")
    @JSONField(format = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime ctime;

    @ApiModelProperty(value = "更新时间")
    @JSONField(format = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime mtime;


}
