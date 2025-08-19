package com.bilibili.cluster.scheduler.api.event.clickhouse.tide;

import cn.hutool.json.JSONUtil;
import com.bilibili.cluster.scheduler.api.event.AbstractTaskEventHandler;
import com.bilibili.cluster.scheduler.api.service.bmr.config.BmrConfigService;
import com.bilibili.cluster.scheduler.api.service.caster.CasterService;
import com.bilibili.cluster.scheduler.api.service.clickhouse.clickhouse.ClickhouseService;
import com.bilibili.cluster.scheduler.common.Constants;
import com.bilibili.cluster.scheduler.common.dto.clickhouse.clickhouse.ClickhouseDeployDTO;
import com.bilibili.cluster.scheduler.common.dto.clickhouse.clickhouse.params.CkTideExtFlowParams;
import com.bilibili.cluster.scheduler.common.dto.clickhouse.clickhouse.templates.ClickhouseCluster;
import com.bilibili.cluster.scheduler.common.entity.ExecutionFlowEntity;
import com.bilibili.cluster.scheduler.common.event.TaskEvent;

import javax.annotation.Resource;
import java.util.List;

/**
 * @description: 潮汐的presto变更并发布
 * @Date: 2024/12/4 17:34
 * @Author: nizhiqiang
 */
public abstract class AbstractCkTideDeployEventHandler extends AbstractTaskEventHandler {

    @Resource
    BmrConfigService bmrConfigService;

    @Resource
    ClickhouseService clickhouseService;

    @Resource
    CasterService casterService;

    @Override
    public boolean executeTaskEvent(TaskEvent taskEvent) throws Exception {

        Long flowId = taskEvent.getFlowId();

        if (initZeroTolerance(taskEvent)) {
            logPersist(taskEvent, "初始化容错度和当前错误节点为0");
            executionFlowService.updateCurFault(flowId, 0);
            executionFlowService.updateFlowTolerance(flowId, 0);
        }

        ExecutionFlowEntity executionFlow = executionFlowService.getById(flowId);

        CkTideExtFlowParams ckTideExtFlowParams = executionFlowPropsService.getFlowExtParamsByCache(flowId, CkTideExtFlowParams.class);

        Long componentId = executionFlow.getComponentId();
//        默认潮汐使用最新的配置的版本

        long configId = ckTideExtFlowParams.getConfigId();
        logPersist(taskEvent, String.format("潮汐使用的配置版本为%s", configId));

        List<Integer> allocationList = getShardAllocationList(taskEvent, ckTideExtFlowParams, configId);
        logPersist(taskEvent, String.format("获取到当前集群的shard分配列表为%s", JSONUtil.toJsonStr(allocationList)));
//        潮汐默认使用clickhouse-stable模版
        ClickhouseDeployDTO clickhouseDeployDTO = clickhouseService.buildScaleDeployDTO(configId, Constants.CK_STABLE_TEMPLATE, allocationList);
        List<ClickhouseCluster> ckClusterList = clickhouseDeployDTO.getChConfig().getClusters();
        logPersist(taskEvent, String.format("修改%s文件,修改后的文件json为%s", Constants.CK_ADMIN_SHARDS_FILE, JSONUtil.toJsonStr(ckClusterList)));
        clickhouseService.updateShardFile(componentId, ckClusterList);

        logPersist(taskEvent, String.format("修改%s文件成功", Constants.CK_ADMIN_SHARDS_FILE));
        logPersist(taskEvent, "发送给caster的对象为:\n" + JSONUtil.toJsonStr(clickhouseDeployDTO));

        casterService.deployClickHouse(clickhouseDeployDTO);
        logPersist(taskEvent, "发起ck变更成功");

        return true;
    }

    /**
     * 是否初始化容错度为0
     *
     * @param taskEvent
     * @return
     */
    protected boolean initZeroTolerance(TaskEvent taskEvent) {
        return false;
    }

    /**
     * 获取需要缩放的pod数量
     *
     * @param params
     * @return
     */
    abstract List<Integer> getShardAllocationList(TaskEvent taskEvent, CkTideExtFlowParams ckTideExtFlowParams, Long configVersionId);
}
