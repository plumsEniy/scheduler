package com.bilibili.cluster.scheduler.api.service.jobAgent;

import com.bilibili.cluster.scheduler.common.dto.jobAgent.JobScriptInfo;
import com.bilibili.cluster.scheduler.common.dto.jobAgent.TaskAtomDetail;
import com.bilibili.cluster.scheduler.common.dto.jobAgent.TaskAtomReport;
import com.bilibili.cluster.scheduler.common.dto.jobAgent.TaskSetData;
import com.bilibili.cluster.scheduler.common.dto.jobAgent.model.JobResult;
import com.bilibili.cluster.scheduler.common.enums.jobAgent.ScriptJobExecuteStateEnum;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

/**
 * @description: jobagent的服务
 * @Date: 2024/5/10 10:49
 * @Author: nizhiqiang
 */
public interface JobAgentService {

    // 查询快速执行taskset执行细节
    TaskSetData getTaskSetSummary(long taskSetId);

    // 根据taskSetId查询批次task执行详情
    List<TaskAtomDetail> getTaskList(long taskSetId);

    // 查询具体task执行详情
    TaskAtomReport getTaskReport(long taskId);

    /**
     * 查询主机job-agent存活状态
     * @param hostList
     * @return
     */
    Map<String, Boolean> queryNodeJobAgentLiveStatus(List<String> hostList);

    /**
     * 查询未安装job-agent节点列表
     * @param hostList
     * @return
     */
    List<String> queryLostJobAgent(List<String> hostList);

}
