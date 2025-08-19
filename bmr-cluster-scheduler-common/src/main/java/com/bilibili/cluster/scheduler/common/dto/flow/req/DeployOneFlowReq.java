package com.bilibili.cluster.scheduler.common.dto.flow.req;

import com.bilibili.cluster.scheduler.common.enums.flow.FlowDeployType;
import com.bilibili.cluster.scheduler.common.enums.flow.FlowEffectiveModeEnum;
import com.bilibili.cluster.scheduler.common.enums.flow.FlowGroupTypeEnum;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import java.util.List;

/**
 * @description: saber任务批量更新的请求
 * @Date: 2024/1/25 19:43
 * @Author: nizhiqiang
 */

@Data
public class DeployOneFlowReq {

    @Positive(message = "flowid必传")
    private Long flowId;

    /**
     * 集群id
     */
    private Long clusterId;

    /**
     * 组件Id
     */
    private Long componentId;

    /**
     * 并发度
     */
    @Positive(message = "并发度错误")
    private Integer parallelism;

    /**
     * 错误容忍度
     */
    private Integer tolerance;

    /**
     * 主机列表,全量发布需要获取机器列表
     */
//    @NotEmpty(message = "主机列表")
    private List<String> nodeList;

    /**
     * 发布类型
     */
    @NotNull(message = "发布类型为空")
    private FlowDeployType deployType;
    /**
     * 包类型
     */
    private String deployPackageType;
    /**
     * 部署类型：全量、灰度
     */
    private String releaseScopeType;

    /**
     * 安装包id
     */
    private Long packageId;
    /**
     * 配置包id
     */
    private Long configId;


    /**
     * 审批人
     */
    //@NotBlank(message = "审批人为空")
    private String approver;

    /**
     * 发布说明
     */
    @NotBlank(message = "发布说明为空")
    private String remark;

    /**
     * 是否重启
     */
    private Boolean restart;

    /**
     * 生效方式
     */
    @NotNull(message = "生效方式为空")
    private FlowEffectiveModeEnum effectiveMode;

    /**
     * 保存之后的参数id
     */
    private long paramId;

    /**
     * 分组方式
     */
    private FlowGroupTypeEnum groupType;

    /**
     * 潮汐发布申请人
     */
    private String userName;
    /**
     * 是否需要审批判断,true为需要
     */
    private String isApproval;

    /**
     * 是否自动重试
     */
    private Boolean autoRetry;

    /**
     * 最大重试次数
     */
    private Integer maxRetry;

    /**
     * 额外参数：json，按需解析展开
     */
    private String extParams;

    /**
     * 组件名
     */
    private String componentName;

    private String roleName;

    private String clusterName;
}
