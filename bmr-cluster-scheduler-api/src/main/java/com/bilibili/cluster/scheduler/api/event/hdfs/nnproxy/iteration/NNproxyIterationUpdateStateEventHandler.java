package com.bilibili.cluster.scheduler.api.event.hdfs.nnproxy.iteration;

import com.bilibili.cluster.scheduler.api.event.hdfs.nnproxy.AbstractNNproxyUpdateStateEventHandler;
import com.bilibili.cluster.scheduler.common.dto.hdfs.nnproxy.parms.NNProxyDeployNodeExtParams;
import com.bilibili.cluster.scheduler.common.enums.event.EventTypeEnum;
import com.bilibili.cluster.scheduler.common.event.TaskEvent;
import org.springframework.stereotype.Component;

/**
 * @description:
 * @Date: 2025/4/29 15:45
 * @Author: nizhiqiang
 */

@Component
public class NNproxyIterationUpdateStateEventHandler extends AbstractNNproxyUpdateStateEventHandler {
    @Override
    public EventTypeEnum getHandleEventType() {
        return EventTypeEnum.NN_PROXY_ITERATION_UPDATE_STATE;
    }

    @Override
    protected String getConfigVersion(TaskEvent taskEvent, NNProxyDeployNodeExtParams nnProxyDeployNodeExtParams) {

        String configVersion = nnProxyDeployNodeExtParams.getConfigVersion();
        if (isInRollbackStatus(taskEvent)) {
            configVersion = nnProxyDeployNodeExtParams.getBeforeConfigVersion();
        }
        return configVersion;
    }

    @Override
    protected String getPackageVersion(TaskEvent taskEvent, NNProxyDeployNodeExtParams nnProxyDeployNodeExtParams) {
        String packageVersion = nnProxyDeployNodeExtParams.getPackageVersion();
        if (isInRollbackStatus(taskEvent)) {
            packageVersion = nnProxyDeployNodeExtParams.getBeforePackageVersion();
        }
        return packageVersion;
    }

    @Override
    protected Long getComponentId(TaskEvent taskEvent, NNProxyDeployNodeExtParams nnProxyDeployNodeExtParams) {
        return nnProxyDeployNodeExtParams.getComponentId();
    }

}
