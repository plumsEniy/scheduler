package com.bilibili.cluster.scheduler.api.event.zk;

import cn.hutool.core.lang.Assert;
import com.bilibili.cluster.scheduler.api.event.AbstractTaskEventHandler;
import com.bilibili.cluster.scheduler.api.service.bmr.config.BmrConfigService;
import com.bilibili.cluster.scheduler.api.service.bmr.resource.BmrResourceService;
import com.bilibili.cluster.scheduler.api.service.flow.ExecutionFlowPropsService;
import com.bilibili.cluster.scheduler.common.Constants;
import com.bilibili.cluster.scheduler.common.dto.bmr.config.ConfigData;
import com.bilibili.cluster.scheduler.common.dto.bmr.resource.model.ResourceHostInfo;
import com.bilibili.cluster.scheduler.common.dto.flow.prop.BaseFlowExtPropDTO;
import com.bilibili.cluster.scheduler.common.entity.ExecutionNodeEntity;
import com.bilibili.cluster.scheduler.common.enums.bmr.config.ConfigFileTypeEnum;
import com.bilibili.cluster.scheduler.common.enums.bmr.config.ConfigVersionType;
import com.bilibili.cluster.scheduler.common.enums.bmr.config.FileOperateType;
import com.bilibili.cluster.scheduler.common.enums.event.EventTypeEnum;
import com.bilibili.cluster.scheduler.common.enums.node.NodeType;
import com.bilibili.cluster.scheduler.common.event.TaskEvent;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @description:
 * @Date: 2025/6/26 15:37
 * @Author: nizhiqiang
 */

@Component
public class ZkEvictionUpdateConfEventHandler extends AbstractTaskEventHandler {

    @Resource
    BmrConfigService bmrConfigService;

    @Resource
    BmrResourceService bmrResourceService;

    @Resource
    ExecutionFlowPropsService executionFlowPropsService;

    /**
     * 仅在阶段1的开始执行
     *
     * @param taskEvent
     * @return
     */
    @Override
    protected boolean checkEventIsRequired(TaskEvent taskEvent) {
        final ExecutionNodeEntity executionNode = taskEvent.getExecutionNode();
        final String execStage = executionNode.getExecStage();
        NodeType nodeType = executionNode.getNodeType();
        if (execStage.equalsIgnoreCase("1") && NodeType.STAGE_START_NODE.equals(nodeType)) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean executeTaskEvent(TaskEvent taskEvent) throws Exception {

        Long componentId = taskEvent.getExecutionFlowInstanceDTO().getExecutionFlowProps().getComponentId();
        Long flowId = taskEvent.getFlowId();

        ConfigData configData = bmrConfigService.queryItemListAndData(null, componentId,
                Constants.ZK_SERVER_CONFIG_FILE_NAME, ConfigFileTypeEnum.STRING, ConfigVersionType.SPECIAL);
        Assert.isTrue(configData != null, "zoo.cfg not found");
        String context = configData.getContext();

        final BaseFlowExtPropDTO baseFlowExtPropDTO = executionFlowPropsService.getFlowPropByFlowId(flowId, BaseFlowExtPropDTO.class);
        List<String> nodeList = baseFlowExtPropDTO.getNodeList();
        List<ResourceHostInfo> resourceHostInfoList = bmrResourceService.queryHostListByName(nodeList);
        List<String> ipList = resourceHostInfoList.stream()
                .map(ResourceHostInfo::getIp)
                .collect(Collectors.toList());

        List<String> ipServerList = new LinkedList<>();
        for (String ip : ipList) {
            Integer ipIndex = findServerNumberByIp(context, ip);
            if (ipIndex == -1){
                logPersist(taskEvent, String.format("zoo.cfg not contains server %s, will skip remove", ip));
                continue;
            }
            String ipServer = String.format("server.%d=%s:2900:3900", ipIndex, ip);
            ipServerList.add(ipServer);
        }

        if (CollectionUtils.isEmpty(ipServerList)){
            logPersist(taskEvent, "zoo.cfg not contains any eviction server, will skip update");
            return true;
        }

        logPersist(taskEvent, String.format("zoo.cfg will add %s", ipServerList));
        bmrConfigService.updateFileIpList(componentId, Constants.ZK_SERVER_CONFIG_FILE_NAME, FileOperateType.REMOVE, ipServerList);
        logPersist(taskEvent, "zoo.cfg update success");

        Thread.sleep(Constants.ONE_SECOND * 5);
        return true;
    }

    private Integer findServerNumberByIp(String context, String targetIp) {
        Pattern pattern = Pattern.compile("^server\\.(\\d+)=" + targetIp + ":.*$", Pattern.MULTILINE);
        Matcher matcher = pattern.matcher(context);

        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1)); // 提前返回匹配的编号
        }

        return -1; // 没有找到匹配项
    }

    @Override
    public EventTypeEnum getHandleEventType() {
        return EventTypeEnum.ZK_EVICTION_UPDATE_CONF;
    }
}
