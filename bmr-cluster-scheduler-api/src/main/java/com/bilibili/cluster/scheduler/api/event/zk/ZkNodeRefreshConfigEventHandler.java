package com.bilibili.cluster.scheduler.api.event.zk;

import cn.hutool.core.lang.Assert;
import com.bilibili.cluster.scheduler.api.event.dolphinScheduler.AbstractDolphinSchedulerEventHandler;
import com.bilibili.cluster.scheduler.api.service.bmr.config.BmrConfigService;
import com.bilibili.cluster.scheduler.common.Constants;
import com.bilibili.cluster.scheduler.common.dto.bmr.config.model.FileDownloadData;
import com.bilibili.cluster.scheduler.common.enums.event.EventTypeEnum;
import com.bilibili.cluster.scheduler.common.event.TaskEvent;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @description: zk其他节点刷新
 * @Date: 2025/7/3 11:49
 * @Author: nizhiqiang
 */

@Component
public class ZkNodeRefreshConfigEventHandler extends AbstractDolphinSchedulerEventHandler {

    @Resource
    private BmrConfigService bmrConfigService;

    @Override
    protected Map<String, Object> getDolphinExecuteEnv(TaskEvent taskEvent, List<String> hostList) {
        Map<String, Object> envMap = super.getDolphinExecuteEnv(taskEvent, hostList);
        Long componentId = taskEvent.getExecutionFlowInstanceDTO().getExecutionFlowProps().getComponentId();

        FileDownloadData fileDownloadData = bmrConfigService.queryDownloadInfoByComponentId(
                componentId, Constants.ZOO_CONFIG);
        Assert.isTrue(!Objects.isNull(fileDownloadData), "zoo.cfg not found");
        envMap.put(Constants.ZK_CONF_URL, fileDownloadData.getDownloadUrl());
        envMap.put(Constants.ZK_CONF_MD5, fileDownloadData.getFileMd5());

        return envMap;
    }

    @Override
    public EventTypeEnum getHandleEventType() {
        return EventTypeEnum.ZK_NODE_REFRESH_CONFIG;
    }
}
