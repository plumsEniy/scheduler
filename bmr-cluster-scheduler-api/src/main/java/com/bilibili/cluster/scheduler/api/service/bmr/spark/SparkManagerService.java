package com.bilibili.cluster.scheduler.api.service.bmr.spark;


import com.bilibili.cluster.scheduler.common.dto.bmr.experiment.ExperimentJobType;
import com.bilibili.cluster.scheduler.common.dto.bmr.experiment.ExperimentJobResultDTO;
import com.bilibili.cluster.scheduler.common.dto.spark.periphery.SparkPeripheryComponent;
import com.bilibili.cluster.scheduler.common.dto.spark.manager.SparkJobInfoDTO;
import com.bilibili.cluster.scheduler.common.dto.spark.periphery.req.SparkPeripheryComponentVersionInfoReq;
import com.bilibili.cluster.scheduler.common.dto.spark.periphery.req.SparkPeripheryComponentVersionUpdateReq;
import com.bilibili.cluster.scheduler.common.dto.spark.periphery.resp.SparkPeripheryComponentVersionInfoDTO;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface SparkManagerService {

    SparkJobInfoDTO querySparkJobInfo(String jobId, ExperimentJobType type, long testSetVersionId);

    /**
     * 查询非minorVersion得非锁定任务
     * @param minorVersion
     * @return
     */
    List<SparkJobInfoDTO> queryAllPublishJobExcludeByVersion(String minorVersion);

    boolean updateSparkVersion(String jobId, String targetSparkVersion);

    boolean lockSparkJobVersion(String jobId, boolean lockOrNot);

    List<String> queryTargetVersionJobList(String originalSparkVersion);

    boolean updateSparkCiJobInfo(ExperimentJobResultDTO jobResultDTO);

    List<String> querySparkClientAllNodes();

    String queryCurrentSparkDefaultVersion();

    /**
     * 查询所有周边组件发布的任务列表（排除目标版本）
     * @param component
     * @param excludeTargetVersion
     * @return
     */
    List<String> queryAllReleaseJobList(SparkPeripheryComponent component, String excludeTargetVersion);

    /**
     * 查询所有周边组件发布的任务列表，根据stage分组（排除目标版本）
     * @param component
     * @param excludeTargetVersion
     * @return
     */
    Map<String, Set<String>> queryAllReleaseStageWithJobs(SparkPeripheryComponent component, String excludeTargetVersion);

    SparkPeripheryComponentVersionInfoDTO querySparkPeripheryComponentVersionInfo(SparkPeripheryComponentVersionInfoReq queryReq);

    boolean updateSparkPeripheryComponentVersion(SparkPeripheryComponentVersionUpdateReq versionUpdateReq);

}
