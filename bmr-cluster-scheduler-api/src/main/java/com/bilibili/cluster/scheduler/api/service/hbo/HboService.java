package com.bilibili.cluster.scheduler.api.service.hbo;

import com.bilibili.cluster.scheduler.common.dto.hbo.model.HboJob;
import com.bilibili.cluster.scheduler.common.dto.hbo.model.HboJobInfo;

import java.util.List;
import java.util.Map;

public interface HboService {

    /**
     * 根据jobid查询jobparams
     *
     * @param jobIdList
     * @return
     */
    List<HboJobInfo> queryJobListByJobId(List<String> jobIdList);

    /**
     * 更新或者新增任务
     *
     * @param jobList
     */
    void upsertJob(List<HboJob> jobList);


    /**
     * 删除job
     *
     * @param jobIdList
     */
    void deleteJob(List<String> jobIdList);

    /**
     * 更新job参数
     * @param jobIdList
     * @param addParamsMap          新增和删除必须有一个
     * @param removeParamsMap
     */
    void updateJobParams(List<String> jobIdList, Map<String,String> addParamsMap, Map<String,String> removeParamsMap);
}
