package com.bilibili.cluster.scheduler.api.service.scheduler;

import com.bilibili.cluster.scheduler.api.exceptions.DolphinSchedulerInvokerException;
import com.bilibili.cluster.scheduler.common.dolphin.DolphinPipelineDefinition;
import com.bilibili.cluster.scheduler.common.dolphin.DolphinPipelineResolveParameter;
import com.bilibili.cluster.scheduler.common.dto.scheduler.ExecutionInstanceDetail;
import com.bilibili.cluster.scheduler.common.dto.scheduler.model.PipelineDefine;
import com.bilibili.cluster.scheduler.common.dto.scheduler.model.SchedTaskDefine;
import com.bilibili.cluster.scheduler.common.dto.scheduler.model.TaskInstance;
import com.bilibili.cluster.scheduler.common.enums.scheduler.DolpFailureStrategy;

import java.util.List;
import java.util.Map;

/**
 * @description: dolphin scheduler
 * @Date: 2024/5/13 17:24
 * @Author: nizhiqiang
 */
public interface DolphinSchedulerInteractService {


    /**
     * 解析taskcode
     * @param projectName
     * @param pipelineName
     * @return
     */
    PipelineDefine queryPipelineDefine(String projectName, String pipelineName) throws DolphinSchedulerInvokerException;


    /**
     * 解析taskcode
     * @param projectCode
     * @param pipelineCode
     * @return
     */
    List<SchedTaskDefine> parsePipelineDefineByCode(String projectCode, String pipelineCode);

    /**
     * 开始pipline
     * @param projectCode
     * @param pipelineCode
     * @param execEnv
     * @param failureStrategy
     * @return
     */
    String startPipeline(String projectCode, String pipelineCode, Map<String, Object> execEnv, DolpFailureStrategy failureStrategy);

    /**
     * 查询实例状态
     * @param projectCode
     * @param schedInstanceId
     * @return
     */
    String querySchedInstanceStatus(String projectCode, String schedInstanceId);

    /**
     * 查询sched任务详情
     * @param projectCode
     * @param schedInstanceId
     * @return
     */
    ExecutionInstanceDetail querySchedInstanceTaskDetail(String projectCode, String schedInstanceId);

    /**
     * 暂停实例
     * @param projectCode
     * @param schedInstanceId
     * @return
     */
    boolean pauseSchedInstance(String projectCode, String schedInstanceId);

    /**
     * 恢复实例
     * @param projectCode
     * @param schedInstanceId
     * @return
     * @throws InterruptedException
     */
    boolean resumeSchedInstance(String projectCode, String schedInstanceId) throws InterruptedException;

    /**
     * 重跑失败任务
     * @return
     */
    boolean retryFailureTask(String projectId, String schedInstanceId);

    /**
     * 通过发布参数解析得到流程定义
     * @param dolphinPipelineResolveParameter
     * @return
     */
    DolphinPipelineDefinition resolvePipelineDefinition(DolphinPipelineResolveParameter dolphinPipelineResolveParameter) throws DolphinSchedulerInvokerException;

    /**
     *  根据projectCode和instanceId查询执行节点状态
     * @param projectId
     * @param instanceId
     */
    List<TaskInstance> queryTaskNodeListExecState(String projectId, String instanceId);
}
