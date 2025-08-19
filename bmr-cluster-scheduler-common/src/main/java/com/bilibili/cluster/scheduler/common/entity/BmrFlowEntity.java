package com.bilibili.cluster.scheduler.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * @description: Bmr工作流
 * @Date: 2024/5/23 10:27
 * @Author: nizhiqiang
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("deploy_execution_flow")
@ApiModel(value = "bmrFlow对象", description = "")
public class BmrFlowEntity extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 自增id
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * uuid
     */
    private String approveUuid;

    /**
     * 集群id
     */
    private Integer clusterId;

    /**
     * 组件id
     */
    private Integer componentId;

    /**
     * 角色名称 yarn, clickhouse, hdfs... etc
     */
    private String roleName;

    /**
     * 组件名称
     */
    private String componentName;

    /**
     * 集群名称
     */
    private String clusterName;
    /**
     * job执行方式
     */
    private String jobExecuteType;

    /**
     * 变更类型：迭代、扩容、上下线
     */
    private String deployType;

    /**
     * 发布类型：全量、灰度、yarn队列等
     */
    private String releaseScopeType;

    /**
     * 包类型：安装包、配置包等
     */
    private String deployPackageType;

    /**
     * 是否重启标记位
     */
    private Boolean restart;

    /**
     * 生效模式：重启｜立即 生效
     */
    private String effectiveMode;

    /**
     * dolphinscheduler的项目id
     */
    private String schedProjectId;

    /**
     * dolphinscheduler的流程id
     */
    private String schedPipelineId;

    /**
     * 资源管理节点组信息
     */
    private Long resourceNodeGroup;

    /**
     * 配置节点组
     */
    private Long configGroup;

    /**
     * 提交者
     */
    private String submitUser;

    /**
     * 审批者
     */
    private String approver;

    /**
     * 发布变更说明
     */
    private String remark;

    /**
     * 部署环境:dev、prod
     */
    private String deployEnv;

    /**
     * 变更申请单No
     */
    private String orderNo;

    /**
     * 变更申请单id
     */
    private String orderId;

    /**
     * 审批状态
     */
    private String applyState;

    /**
     * 并发度
     */
    private Integer parallelism;

    /**
     * 安装包版本id
     */
    private Long packageId;

    /**
     * 配置包版本id
     */
    private Long configId;

    /**
     * 错误容忍度
     */
    private Integer tolerance;

    /**
     * 当前失败节点数量
     */
    private Integer curFault;

    /**
     * 开始执行时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    /**
     * 结束执行时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;

    /**
     * 枚举类型字段：工作流执行状态
     */
    private String flowState;

    /**
     * 执行策略
     */
    private String opStrategy;

    /**
     * 日志id
     */
    private Long logId;

    /**
     * 队列发布id
     */
    private Long queueId;

    /**
     * 队列发布种类（day or night）
     */
    private String queueEffectiveName;

    /**
     * 执行到的批次id
     */
    private int curBatchId;

    /**
     * 最新活跃时间
     */
    @JsonFormat(pattern="yyyy-MM-dd HH:mm:ss", timezone="GMT+8")
    private LocalDateTime latestActiveTime;

    /**
     * 分组方式
     */
    private String groupType;

    /**
     * 事件运营中心的c_uuid
     */
    private String incidentTransferUuid;

    /**
     * 额外componentId，configId，pakageId
     */
    private String extraEnv;

    @TableField(exist = false)
    private String podTemplate;
    @TableField(exist = false)
    private Integer count;

    @TableField(exist = false)
    private String imageName;

}