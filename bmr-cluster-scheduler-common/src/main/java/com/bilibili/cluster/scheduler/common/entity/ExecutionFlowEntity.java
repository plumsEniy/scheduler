package com.bilibili.cluster.scheduler.common.entity;

import com.alibaba.fastjson.annotation.JSONField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;

import java.time.LocalDateTime;
import java.io.Serializable;

import com.bilibili.cluster.scheduler.common.enums.flow.*;
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
@TableName("scheduler_execution_flow_v2")
@ApiModel(value = "ExecutionFlow对象", description = "")
public class ExecutionFlowEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "自增id")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @ApiModelProperty(value = "封网审批的唯一id")
    private String approveUuid;

    @ApiModelProperty(value = "集群id")
    private Long clusterId;

    @ApiModelProperty(value = "组件id")
    private Long componentId;

    @ApiModelProperty(value = "角色名：yarn, clickhouse, hdfs... etc")
    private String roleName;

    @ApiModelProperty(value = "组件名")
    private String componentName;

    @ApiModelProperty(value = "集群名")
    private String clusterName;

    @ApiModelProperty(value = "变更类型：迭代、扩容、上下线")
    private FlowDeployType deployType;

    @ApiModelProperty(value = "发布类型：全量、灰度、yarn队列等")
    private String releaseScopeType;

    @ApiModelProperty(value = "包类型：安装包、配置包等")
    private String deployPackageType;

    @ApiModelProperty(value = "是否重启标记位")
    private Boolean restart;

    @ApiModelProperty(value = "生效模式：重启｜立即 生效")
    private FlowEffectiveModeEnum effectiveMode;

    /**
     * 审批者
     */
    private String approver;

    /**
     * 安装包版本id
     */
    private Long packageId;

    /**
     * 配置包版本id
     */
    private Long configId;

    /**
     * job执行方式
     */
    private String jobExecuteType;

    @ApiModelProperty(value = "是否自动重试")
    private Boolean autoRetry = true;

    @ApiModelProperty(value = "最大重试次数")
    private Integer maxRetry = 1;

    @ApiModelProperty(value = "并发度")
    private Integer parallelism;

    @ApiModelProperty(value = "容错度")
    private Integer tolerance;

    @ApiModelProperty(value = "当前失败节点数")
    private Integer curFault;

    @ApiModelProperty(value = "开始时间")
    @JSONField(format = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    @ApiModelProperty(value = "结束时间")
    @JSONField(format = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;

    @ApiModelProperty(value = "枚举类型字段：工作流执行状态")
    private FlowStatusEnum flowStatus;

    @ApiModelProperty(value = "创建时间")
    @JSONField(format = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime ctime;

    @ApiModelProperty(value = "更新时间")
    @JSONField(format = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime mtime;

    @ApiModelProperty(value = "删除标识")
    private Boolean deleted;

    @ApiModelProperty(value = "最新活跃时间")
    @JSONField(format = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime latestActiveTime;

    @ApiModelProperty(value = "工作流名称")
    private String flowName;

    @ApiModelProperty(value = "发布单说明")
    private String flowRemark;

    @ApiModelProperty(value = "当前执行批次号")
    private Integer curBatchId;

    @ApiModelProperty(value = "最大批次号")
    private Integer maxBatchId;

    @ApiModelProperty(value = "提交者")
    private String operator;

    @ApiModelProperty(value = "参数id")
    private Long propsId;

    @ApiModelProperty(value = "执行实例主机")
    private String hostName;

    @ApiModelProperty(value = "审批单id")
    private String orderId;

    @ApiModelProperty(value = "审批单No")
    private String orderNo;

    @ApiModelProperty(value = "事件列表")
    private String eventList;

    @ApiModelProperty(value = "节点分组方式")
    private FlowGroupTypeEnum groupType;

    @ApiModelProperty(value = "回滚类型")
    private FlowRollbackType flowRollbackType;

}
