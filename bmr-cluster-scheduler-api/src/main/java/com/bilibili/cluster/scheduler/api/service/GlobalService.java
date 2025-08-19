package com.bilibili.cluster.scheduler.api.service;

import com.bilibili.cluster.scheduler.api.service.bmr.config.BmrConfigService;
import com.bilibili.cluster.scheduler.api.service.bmr.metadata.BmrMetadataService;
import com.bilibili.cluster.scheduler.api.service.bmr.resource.BmrResourceService;
import com.bilibili.cluster.scheduler.api.service.bmr.resourceV2.BmrResourceV2Service;
import com.bilibili.cluster.scheduler.api.service.flow.ExecutionFlowService;
import com.bilibili.cluster.scheduler.api.service.flow.ExecutionLogService;
import com.bilibili.cluster.scheduler.api.service.flow.ExecutionNodeEventService;
import com.bilibili.cluster.scheduler.api.service.jobAgent.JobAgentService;
import com.bilibili.cluster.scheduler.api.service.scheduler.DolphinSchedulerInteractService;
import com.bilibili.cluster.scheduler.api.service.bmr.spark.ess.SparkEssMasterService;
import com.bilibili.cluster.scheduler.common.response.ResponseResult;
import lombok.Data;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;

/**
 * @description: 全局service
 * @Date: 2024/5/15 16:50
 * @Author: nizhiqiang
 */
@Component
@Data
public class GlobalService {

    @Resource
    public BmrConfigService bmrConfigService;

    @Resource
    public BmrMetadataService bmrMetadataService;

    @Resource
    public BmrResourceService bmrResourceService;

    @Resource
    public DolphinSchedulerInteractService dolphinSchedulerInteractService;

    @Resource
    public ExecutionLogService executionLogService;

    @Resource
    public JobAgentService jobAgentService;

    @Resource
    public ExecutionNodeEventService executionNodeEventService;

    @Resource
    public ExecutionFlowService executionFlowService;

    @Resource
    BmrResourceV2Service bmrResourceV2Service;

    @Resource
    SparkEssMasterService sparkEssMasterService;

}
