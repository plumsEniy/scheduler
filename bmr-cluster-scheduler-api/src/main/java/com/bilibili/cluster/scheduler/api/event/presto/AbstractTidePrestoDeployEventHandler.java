package com.bilibili.cluster.scheduler.api.event.presto;

import com.bilibili.cluster.scheduler.api.event.AbstractTaskEventHandler;
import com.bilibili.cluster.scheduler.api.service.bmr.config.BmrConfigService;
import com.bilibili.cluster.scheduler.api.service.bmr.metadata.BmrMetadataService;
import com.bilibili.cluster.scheduler.api.service.presto.PrestoService;
import com.bilibili.cluster.scheduler.common.Constants;
import com.bilibili.cluster.scheduler.common.dto.bmr.config.model.ConfigItem;
import com.bilibili.cluster.scheduler.common.dto.bmr.config.req.UpdateSpecialKeyValueFileReq;
import com.bilibili.cluster.scheduler.common.dto.bmr.metadata.MetadataClusterData;
import com.bilibili.cluster.scheduler.common.dto.bmr.metadata.MetadataPackageData;
import com.bilibili.cluster.scheduler.common.entity.ExecutionFlowEntity;
import com.bilibili.cluster.scheduler.common.event.TaskEvent;
import org.apache.http.util.Asserts;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * @description: 潮汐的presto变更并发布
 * @Date: 2024/12/4 17:34
 * @Author: nizhiqiang
 */
public abstract class AbstractTidePrestoDeployEventHandler extends AbstractTaskEventHandler {

    @Resource
    BmrMetadataService bmrMetadataService;

    @Resource
    BmrConfigService bmrConfigService;

    @Resource
    PrestoService prestoService;

    @Override
    public boolean executeTaskEvent(TaskEvent taskEvent) throws Exception {
        Long flowId = taskEvent.getFlowId();

        if (initZeroTolerance(taskEvent)) {
            logPersist(taskEvent, "初始化容错度和当前错误节点为0");
            executionFlowService.updateCurFault(flowId, 0);
            executionFlowService.updateFlowTolerance(flowId, 0);
        }

        Long componentId = getComponentId(taskEvent);
        Long clusterId = getClusterId(taskEvent);

        UpdateSpecialKeyValueFileReq req = new UpdateSpecialKeyValueFileReq();
        req.setComponentId(componentId);
        req.setFileName(Constants.WORKER_FILE);
        Integer needScalePodCount = getNeedScalePodCount(taskEvent);

        List<ConfigItem> configItemList = new ArrayList<>();
        ConfigItem configItem = new ConfigItem();
        configItem.setConfigItemKey(Constants.PRESTO_COUNT);
        configItem.setConfigItemValue(String.valueOf(needScalePodCount));
        configItemList.add(configItem);
        req.setUpdateItems(configItemList);

        MetadataClusterData cluster = bmrMetadataService.queryClusterDetail(clusterId);
        logPersist(taskEvent,String.format("发布集群名称为%s", cluster.getClusterName()));
        bmrConfigService.updateSpecialKeyValueFile(req);
        logPersist(taskEvent, String.format("更新worker.xml文件，配置中worker的数量修改为%s", needScalePodCount));

        Long runningPackageId = cluster.getRunningPackageId();
        Long runningConfigId = cluster.getRunningConfigId();
        MetadataPackageData packageData = bmrMetadataService.queryPackageDetailById(runningPackageId);
        Asserts.notNull(packageData, "无法查询到运行中安装包");
        String imagePath = packageData.getImagePath();

        String casterTemplate = prestoService.getDeployPrestoTemplate(clusterId, runningConfigId, imagePath);
        logPersist(taskEvent, "部署的yaml文件为:\n" + prestoService.queryPrestoTemplate(clusterId, runningConfigId, imagePath));
        logPersist(taskEvent, "发送给caster的模版为:\n" + casterTemplate);

        prestoService.deployPresto(clusterId, runningConfigId, imagePath);
        logPersist(taskEvent, "发起presto变更成功");

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
    abstract protected Integer getNeedScalePodCount(TaskEvent taskEvent);

    protected Long getComponentId(TaskEvent taskEvent) {
        Long flowId = taskEvent.getFlowId();
        ExecutionFlowEntity executionFlow = executionFlowService.getById(flowId);
        return executionFlow.getComponentId();
    }

    protected Long getClusterId(TaskEvent taskEvent) {
        Long flowId = taskEvent.getFlowId();
        ExecutionFlowEntity executionFlow = executionFlowService.getById(flowId);
        return executionFlow.getClusterId();
    }
}
