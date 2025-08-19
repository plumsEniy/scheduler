package com.bilibili.cluster.scheduler.api.event.spark.client;

import cn.hutool.json.JSONUtil;
import com.bilibili.cluster.scheduler.api.event.dolphinScheduler.AbstractDolphinSchedulerEventHandler;
import com.bilibili.cluster.scheduler.common.Constants;
import com.bilibili.cluster.scheduler.common.dto.bmr.metadata.MetadataPackageData;
import com.bilibili.cluster.scheduler.common.dto.spark.client.SparkClientDeployExtParams;
import com.bilibili.cluster.scheduler.common.dto.spark.client.SparkClientPackInfo;
import com.bilibili.cluster.scheduler.common.dto.spark.client.SparkClientType;
import com.bilibili.cluster.scheduler.common.enums.event.EventTypeEnum;
import com.bilibili.cluster.scheduler.common.event.TaskEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

@Slf4j
@Component
public class SparkClientPackRemoveEventHandler extends AbstractDolphinSchedulerEventHandler {

    @Override
    protected Map<String, Object> getDolphinExecuteEnv(TaskEvent taskEvent, List<String> hostList) {
        Map<String, Object> evnMap = new HashMap<>();
        StringJoiner joiner = new StringJoiner(Constants.COMMA);
        hostList.forEach(joiner::add);
        // 机器列表
        String hosts = joiner.toString();
        evnMap.put(Constants.SYSTEM_JOBAGENT_EXEC_HOSTS, hosts);
        logPersist(taskEvent, "execute hosts is: \n" + hosts);

        final Long flowId = taskEvent.getFlowId();
        final SparkClientDeployExtParams flowExtParams = executionFlowPropsService.getFlowExtParamsByCache(flowId, SparkClientDeployExtParams.class);
        final List<Long> packIdList = flowExtParams.getPackIdList();
        List<SparkClientPackInfo> packInfoList = new ArrayList<>();

        for (Long packId : packIdList) {
            final MetadataPackageData packageData = globalService.getBmrMetadataService().queryPackageDetailById(packId);
            final String componentName = packageData.getComponentName();
            final SparkClientType clientType = SparkClientType.getByComponentName(componentName);

            final SparkClientPackInfo clientPackInfo = new SparkClientPackInfo();
            clientPackInfo.setPackName(packageData.getTagName());
            clientPackInfo.setPackMd5(packageData.getProductBagMd5());
            clientPackInfo.setDownloadUrl(packageData.getStoragePath());
            clientPackInfo.setClientType(clientType.getDesc());

            packInfoList.add(clientPackInfo);
        }
        String packInfos = JSONUtil.toJsonStr(packInfoList);
        logPersist(taskEvent, "remove package info list is : \n" + packInfos);
        logPersist(taskEvent, "env of [SPARK_CLIENT_PACK_LIST] value size is: " + packInfos.length());
        // job env of value can not bigger than 3000 chars
        evnMap.put(Constants.SPARK_CLIENT_PACK_LIST_KEY, packInfos);
        return evnMap;
    }

    @Override
    public int getMinLoopWait() {
        return 3_000;
    }

    @Override
    public int getMaxLoopStep() {
        return 5_000;
    }

    @Override
    public EventTypeEnum getHandleEventType() {
        return EventTypeEnum.SPARK_CLIENT_PACK_REMOVE_EVENT;
    }
}
