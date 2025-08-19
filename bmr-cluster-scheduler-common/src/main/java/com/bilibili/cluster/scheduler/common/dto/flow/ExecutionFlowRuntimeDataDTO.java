package com.bilibili.cluster.scheduler.common.dto.flow;

import com.bilibili.cluster.scheduler.common.dto.button.OpStrategyButton;
import com.bilibili.cluster.scheduler.common.dto.button.StageStatePoint;
import com.bilibili.cluster.scheduler.common.entity.ExecutionFlowEntity;
import com.bilibili.cluster.scheduler.common.enums.flow.FlowStatusEnum;
import lombok.Data;

import java.util.List;

/**
 * @description: flow运行数据
 * @Date: 2024/2/1 15:45
 * @Author: nizhiqiang
 */
@Data
public class ExecutionFlowRuntimeDataDTO {

    Long flowId;

    /**
     * 工作流状态
     */
    FlowStatusEnum flowStatus;

    /**
     * 工作流日志
     */
    String flowLog;

    /**
     * 成功节点数量
     */
    private int successCnt;

    /**
     * 运行中节点数量
     */
    private int runningCnt;

    /**
     * 跳过节点数
     */
    private int skipCnt;

    /**
     * rollback节点数
     */
    private int rollbackCnt;

    /**
     * 失败节点数量
     */
    private int failedCnt;

    /**
     * job 总数
     */
    private int totalCnt;

    /**
     * 原版本数
     */
    private int originCnt;

    /**
     * 完成数
     */
    private int finishCnt;

    /**
     * 未执行数
     */
    private int unExecutionCnt;

    /**
     * 当前阶段
     */
    private String curStage;

    /**
     * 总阶段
     */
    private String totalStage;


    /**
     * 发布成功百分比
     */
    private String successPercent;

    /**
     * 还未成功百分比
     */
    private String unSuccessPercent;

    /**
     * 可用按钮
     */
    private List<OpStrategyButton> buttonList;

    /**
     * flow信息
     */
    private ExecutionFlowEntity flowEntity;

    /**
     * stage状态列表
     */
    private List<StageStatePoint> stageStatePointList;

    /**
     * 允许点击继续的最小时间
     */
    private String allowedNextProceedTime;

}
