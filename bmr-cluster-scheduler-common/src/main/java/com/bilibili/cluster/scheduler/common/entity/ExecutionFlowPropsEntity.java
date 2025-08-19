package com.bilibili.cluster.scheduler.common.entity;

import com.alibaba.fastjson.annotation.JSONField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import java.time.LocalDateTime;
import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonFormat;
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
 * @since 2024-01-25
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("scheduler_execution_flow_props")
@ApiModel(value="ExecutionFlowProps对象", description="")
public class ExecutionFlowPropsEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "自增id")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @ApiModelProperty(value = "flow id")
    private Long flowId;

    @ApiModelProperty(value = "属性内容json")
    private String propsContent;

    @ApiModelProperty(value = "创建时间")
    @JSONField(format = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime ctime;

    @ApiModelProperty(value = "更新时间")
    @JSONField(format = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime mtime;

    @ApiModelProperty(value = "删除标识")
    private Boolean deleted;
}
