package com.bilibili.cluster.scheduler.api.service.flow.prepare.factory.monitor;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.json.JSONUtil;
import com.bilibili.cluster.scheduler.api.event.analyzer.PipelineParameter;
import com.bilibili.cluster.scheduler.api.event.analyzer.ResolvedEvent;
import com.bilibili.cluster.scheduler.api.event.factory.FactoryDiscoveryUtils;
import com.bilibili.cluster.scheduler.api.event.factory.PipelineFactory;
import com.bilibili.cluster.scheduler.api.service.bmr.resourceV2.BmrResourceV2Service;
import com.bilibili.cluster.scheduler.api.service.flow.*;
import com.bilibili.cluster.scheduler.api.service.flow.prepare.factory.FlowPrepareGenerateFactory;
import com.bilibili.cluster.scheduler.api.service.metric.MetricService;
import com.bilibili.cluster.scheduler.common.Constants;
import com.bilibili.cluster.scheduler.common.dto.bmr.resourceV2.RmsHostInfo;
import com.bilibili.cluster.scheduler.common.dto.bmr.resourceV2.model.ComponentHostRelationModel;
import com.bilibili.cluster.scheduler.common.dto.bmr.resourceV2.req.QueryComponentHostPageReq;
import com.bilibili.cluster.scheduler.common.dto.flow.prop.BaseFlowExtPropDTO;
import com.bilibili.cluster.scheduler.common.dto.flow.req.DeployOneFlowReq;
import com.bilibili.cluster.scheduler.common.dto.metric.dto.MetricConfInfo;
import com.bilibili.cluster.scheduler.common.dto.metric.dto.MetricHostInfo;
import com.bilibili.cluster.scheduler.common.dto.metric.dto.MetricNodeInstance;
import com.bilibili.cluster.scheduler.common.dto.parameters.dto.flow.metric.MetricExtParams;
import com.bilibili.cluster.scheduler.common.dto.parameters.dto.node.monitor.MonitorNodeParams;
import com.bilibili.cluster.scheduler.common.entity.ExecutionFlowEntity;
import com.bilibili.cluster.scheduler.common.entity.ExecutionNodeEntity;
import com.bilibili.cluster.scheduler.common.enums.NodeExecuteStatusEnum;
import com.bilibili.cluster.scheduler.common.enums.flow.FlowDeployType;
import com.bilibili.cluster.scheduler.common.enums.flow.FlowStatusEnum;
import com.bilibili.cluster.scheduler.common.enums.flowLog.LogTypeEnum;
import com.bilibili.cluster.scheduler.common.enums.metric.MetricEnvEnum;
import com.bilibili.cluster.scheduler.common.enums.metric.MetricModifyType;
import com.bilibili.cluster.scheduler.common.enums.node.NodeOperationResult;
import com.bilibili.cluster.scheduler.common.utils.MonitorConfUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import javax.annotation.Nullable;
import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Component
public class MonitorDeployFlowPrepareGenerateFactory implements FlowPrepareGenerateFactory {

    @Resource
    BmrResourceV2Service bmrResourceV2Service;

    @Resource
    ExecutionNodeService executionNodeService;

    @Resource
    ExecutionNodeEventService executionNodeEventService;

    @Resource
    ExecutionNodePropsService executionNodePropsService;

    @Resource
    ExecutionFlowPropsService executionFlowPropsService;

    @Resource
    ExecutionLogService executionLogService;

    @Resource
    MetricService metricService;

    @Resource
    ExecutionFlowService flowService;

    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRES_NEW)
    public void generateNodeAndEvents(ExecutionFlowEntity flowEntity) throws Exception {
        Long flowId = flowEntity.getId();

        flowEntity = flowService.getById(flowId);
        final FlowStatusEnum flowStatus = flowEntity.getFlowStatus();
        if (flowStatus != FlowStatusEnum.PREPARE_EXECUTE) {
            log.error("un-expected flowStatus of {} when prepare generateNodeAndEvents.", flowStatus);
            return;
        }

        BaseFlowExtPropDTO flowProps = executionFlowPropsService.getFlowPropByFlowId(flowId, BaseFlowExtPropDTO.class);
        String flowExtParams = flowProps.getFlowExtParams();

        try {
            MetricExtParams metricExtParams = JSONUtil.toBean(flowExtParams, MetricExtParams.class);
            Integer integrationId = metricExtParams.getIntegrationId();
            List<MetricConfInfo> afterMonitorList = metricExtParams.getAfterList();
            MetricEnvEnum envType = metricExtParams.getEnvType();

//            主机名和host相关的map
            HashMap<String, MetricHostInfo> hostNameToHostInfoMap = new HashMap<>();
//            所有更新后的监控实例列表
            List<MetricNodeInstance> afterMonitorInstanceList = new ArrayList<>();
            for (MetricConfInfo metricConfInfo : afterMonitorList) {
                QueryComponentHostPageReq req = new QueryComponentHostPageReq();
                req.setComponentId(metricConfInfo.getComponentId());
                req.setClusterId(metricConfInfo.getClusterId());
                List<ComponentHostRelationModel> componentHostRelationList = bmrResourceV2Service.queryComponentHostList(req);

                List<String> unRunningHostNameList = new ArrayList<>();
                Iterator<ComponentHostRelationModel> iterator = componentHostRelationList.iterator();
                while (iterator.hasNext()) {
                    ComponentHostRelationModel componentHostRelation = iterator.next();
                    String applicationState = componentHostRelation.getApplicationState();
                    String ip = componentHostRelation.getIp();
                    String hostName = componentHostRelation.getHostName();

//                    如果ip为空则单独去rms上查询
                    if (StringUtils.isEmpty(ip)) {
                        RmsHostInfo rmsHostInfo = bmrResourceV2Service.queryHostRmsInfo(hostName);
                        List<String> ipList = Optional.ofNullable(rmsHostInfo)
                                .map(RmsHostInfo::getPrivateIPv4)
                                .orElse(null);
                        if (!CollectionUtils.isEmpty(ipList)){
                            componentHostRelation.setIp(ipList.get(0));
                        }
                        String zone = Optional.ofNullable(rmsHostInfo)
                                .map(RmsHostInfo::getIdc)
                                .map(RmsHostInfo.IdcDTO::getZone)
                                .map(RmsHostInfo.ZoneDTO::getCode)
                                .orElse(Constants.NAN);

                        componentHostRelation.setZone(zone);
                    }

                    if (Constants.LOST_APPLICATION_STATE.equals(applicationState) || Constants.UNHEALTHY_APPLICATION_STATE.equals(applicationState)) {
                        unRunningHostNameList.add(hostName);
                        iterator.remove();
                    }
                }

                if (!CollectionUtils.isEmpty(unRunningHostNameList)) {
                    executionLogService.updateLogContent(flowId, LogTypeEnum.FLOW, String.format("lost and un healthy host has been remove, host list is %s", unRunningHostNameList));
                }

                log.info("QueryComponentHostPageReq is {}, host size is {}", JSONUtil.toJsonStr(req), componentHostRelationList.size());

                afterMonitorInstanceList.addAll(MonitorConfUtil.getNodeInstanceList(componentHostRelationList, metricConfInfo));
                log.info("afterMonitorInstanceList size is {}", afterMonitorInstanceList.size());

                componentHostRelationList.forEach(hostRelation -> {
                    hostNameToHostInfoMap.putIfAbsent(hostRelation.getHostName(), new MetricHostInfo(hostRelation.getHostName(), hostRelation.getIp(), hostRelation.getRack()));
                });
            }

//            主机变更后监控列表
            Map<String, List<MetricNodeInstance>> hostNameToAfterMonitorInstancesMap = afterMonitorInstanceList.stream().collect(Collectors.groupingBy(MetricNodeInstance::getName));

            List<MetricNodeInstance> metricNodeInstanceList = metricService.queryMetricInstanceList(envType, integrationId);
//            当前的监控列表
            Map<String, List<MetricNodeInstance>> hostNameToCurrentMonitorInstanceMap = metricNodeInstanceList.stream().collect(Collectors.groupingBy(MetricNodeInstance::getName));

            HashMap<String, MonitorNodeParams> hostNameToParamsMap = generateHostNameToParamsList(metricExtParams, hostNameToAfterMonitorInstancesMap, hostNameToCurrentMonitorInstanceMap);

            int batchId = 1;
            int currs = 0;
            List<ExecutionNodeEntity> executionNodeList = new ArrayList<>();
            Integer flowParallelism = flowEntity.getParallelism();
            log.info("hostNameToParamsMap size is {}", hostNameToParamsMap.size());

            if (CollectionUtils.isEmpty(hostNameToParamsMap)) {
                return;
            }

            for (String hostName : hostNameToParamsMap.keySet()) {
                MetricHostInfo hostInfo = hostNameToHostInfoMap.get(hostName);
//                删除已经作废的节点时，可能无法在组件中查到节点信息
                String ip = Optional.ofNullable(hostInfo).map(MetricHostInfo::getIp).orElse(Constants.EMPTY_STRING);
                String rack = Optional.ofNullable(hostInfo).map(MetricHostInfo::getRack).orElse(Constants.EMPTY_STRING);

                ExecutionNodeEntity executionNodeEntity = new ExecutionNodeEntity();
                executionNodeEntity.setNodeName(hostName);
                executionNodeEntity.setFlowId(flowId);
                executionNodeEntity.setBatchId(batchId);
                executionNodeEntity.setOperator(flowEntity.getOperator());
                executionNodeEntity.setNodeStatus(NodeExecuteStatusEnum.UN_NODE_EXECUTE);
                executionNodeEntity.setOperationResult(NodeOperationResult.NORMAL);
                executionNodeEntity.setRack(rack);
                executionNodeEntity.setIp(ip);
                executionNodeList.add(executionNodeEntity);
                if (++currs >= flowParallelism) {
                    currs = 0;
                    batchId++;
                }
            }
            log.info("executionNodeList node size is {}", executionNodeList.size());

            List<List<ExecutionNodeEntity>> splitList = ListUtil.split(executionNodeList, 500);
            for (List<ExecutionNodeEntity> split : splitList) {
                Assert.isTrue(executionNodeService.batchInsert(split), "保存执行节点信息失败");
            }

            log.info("save flow {} execution node list success.", flowId);

            /**
             * 更新节点参数
             */

            for (ExecutionNodeEntity executionNode : executionNodeList) {
                String hostName = executionNode.getNodeName();
                MonitorNodeParams monitorNodeParams = hostNameToParamsMap.get(hostName);
                executionNodePropsService.saveNodeProp(executionNode.getId(), monitorNodeParams);
            }
            log.info("save flow {} execution node props success.", flowId);

            executionNodeEventService.batchSaveExecutionNodeEvent(flowId, executionNodeList, resolvePipelineEventList(null, flowEntity));
            log.info("save flow {} execution job event success.", flowId);

        } catch (Exception e) {
            log.error(String.format("generate monitor error, flow id is %s, monitor is %s, error is %s", flowId, flowExtParams, e.getMessage()), e);
            throw e;
        }
    }

    /**
     * 生成主机->变更属性的列表，如果主机不需要删除或者新增监控则不会在列表中
     *
     * @param metricExtParams
     * @param hostNameToAfterMonitorInstancesMap
     * @param hostNameToCurrentMonitorInstanceMap
     * @return
     */
    private HashMap<String, MonitorNodeParams> generateHostNameToParamsList(MetricExtParams metricExtParams
            , Map<String, List<MetricNodeInstance>> hostNameToAfterMonitorInstancesMap
            , Map<String, List<MetricNodeInstance>> hostNameToCurrentMonitorInstanceMap) {
        HashMap<String, MonitorNodeParams> hostNameToParamsMap = new HashMap<>();
        Integer integrationId = metricExtParams.getIntegrationId();

//            全量修改的时候，需要删除修改前不存在的主机
        MetricModifyType modifyType = metricExtParams.getModifyType();
        if (MetricModifyType.CRON_SYNC_JOB.equals(modifyType)) {
            for (Map.Entry<String, List<MetricNodeInstance>> entry : hostNameToCurrentMonitorInstanceMap.entrySet()) {
                String hostName = entry.getKey();
                List<MetricNodeInstance> monitorInstanceList = entry.getValue();
                if (!hostNameToAfterMonitorInstancesMap.containsKey(hostName)) {
                    MonitorNodeParams monitorNodeParams = new MonitorNodeParams();
                    monitorNodeParams.setToken(metricExtParams.getToken());
                    monitorNodeParams.setIntegrationId(integrationId);
                    monitorNodeParams.setMonitorEnv(metricExtParams.getEnvType());
                    monitorNodeParams.setRemoveMonitorInstanceList(monitorInstanceList);
                    hostNameToParamsMap.put(hostName, monitorNodeParams);
                }
            }
        }

//            获取需要变更的主机列表和参数信息
        for (String hostName : hostNameToAfterMonitorInstancesMap.keySet()) {
            List<MetricNodeInstance> nodeAfterMonitorInstanceList = hostNameToAfterMonitorInstancesMap.get(hostName);
            List<MetricNodeInstance> nodeCurrentMonitorInstanceList = hostNameToCurrentMonitorInstanceMap.get(hostName);
            MonitorNodeParams monitorNodeParams = generateMonitorNodeParams(metricExtParams, nodeCurrentMonitorInstanceList, nodeAfterMonitorInstanceList);
//                当没有新增和删除的时候就不处理该节点
            if (CollectionUtils.isEmpty(monitorNodeParams.getAddMonitorInstanceList()) && CollectionUtils.isEmpty(monitorNodeParams.getRemoveMonitorInstanceList())) {
                continue;
            }
            hostNameToParamsMap.put(hostName, monitorNodeParams);
        }

        return hostNameToParamsMap;
    }

    private MonitorNodeParams generateMonitorNodeParams(MetricExtParams metricExtParams, List<MetricNodeInstance> nodeCurrentMonitorInstanceList
            , List<MetricNodeInstance> nodeAfterMonitorInstanceList) {
        MetricModifyType modifyType = metricExtParams.getModifyType();
        MetricConfInfo beforeMonitorConfig = metricExtParams.getBefore();
        String token = metricExtParams.getToken();
        Integer integrationId = metricExtParams.getIntegrationId();

        MonitorNodeParams monitorNodeParams = new MonitorNodeParams();
        monitorNodeParams.setToken(token);
        monitorNodeParams.setIntegrationId(integrationId);
        monitorNodeParams.setMonitorEnv(metricExtParams.getEnvType());
        List<MetricNodeInstance> addMonitorInstanceList = new ArrayList<>();
        List<MetricNodeInstance> removeMonitorInstanceList = new ArrayList<>();

        Map<Integer, MetricNodeInstance> portToCurrentMonitorInstanceMap = new HashMap<>();
        if (!CollectionUtils.isEmpty(nodeCurrentMonitorInstanceList)) {
            portToCurrentMonitorInstanceMap = nodeCurrentMonitorInstanceList.stream().collect(Collectors.toMap(MetricNodeInstance::getPort, Function.identity(), (o1, o2) -> o2));
        }
        Map<Integer, MetricNodeInstance> portToAfterMonitorInstanceMap = new HashMap<>();
        if (!CollectionUtils.isEmpty(nodeAfterMonitorInstanceList)) {
            portToAfterMonitorInstanceMap = nodeAfterMonitorInstanceList.stream().collect(Collectors.toMap(MetricNodeInstance::getPort, Function.identity(), (o1, o2) -> o2));
        }

        switch (modifyType) {
            case ADD_MONITOR_CONF:
                addMonitorInstanceList = nodeAfterMonitorInstanceList;
                break;
            case REMOVE_MONITOR_CONF:
                removeMonitorInstanceList = nodeAfterMonitorInstanceList;
                break;
            case MODIFY_MONITOR_CONF:
                addMonitorInstanceList = nodeAfterMonitorInstanceList;

//                        如果当前不存在监控对象则不需要删除只进行新增
                if (CollectionUtils.isEmpty(nodeCurrentMonitorInstanceList)) {
                    break;
                }
                List<String> beforeMonitorPortList = Arrays.asList(beforeMonitorConfig.getPorts().split(Constants.COMMA));

                for (String beforePort : beforeMonitorPortList) {
                    Integer port = Integer.valueOf(beforePort);

                    MetricNodeInstance currentMonitor = portToCurrentMonitorInstanceMap.get(port);
//                    当前没有这个监控端口监控则跳过
                    if (Objects.isNull(currentMonitor)) {
                        continue;
                    }

//                    如果修改后不存在则进行跳过
                    MetricNodeInstance afterMonitor = portToAfterMonitorInstanceMap.get(port);
                    if (Objects.isNull(afterMonitor)) {
                        removeMonitorInstanceList.add(currentMonitor);
                        continue;
                    }

//                    如果相同则不用删除，也不用添加
                    if (afterMonitor.equals(currentMonitor)) {
                        addMonitorInstanceList.remove(afterMonitor);
                        continue;
                    }
//                    不相同则需要删除后再增加
                    removeMonitorInstanceList.add(currentMonitor);
                }
                break;
            case CRON_SYNC_JOB:
                addMonitorInstanceList = nodeAfterMonitorInstanceList;
                //                        如果当前不存在监控对象则不需要删除只进行新增
                if (CollectionUtils.isEmpty(nodeCurrentMonitorInstanceList)) {
                    break;
                }

                for (Integer port : portToCurrentMonitorInstanceMap.keySet()) {

                    MetricNodeInstance currentMonitor = portToCurrentMonitorInstanceMap.get(port);

//                    如果修改后不存在则进行移除
                    MetricNodeInstance afterMonitor = portToAfterMonitorInstanceMap.get(port);
                    if (Objects.isNull(afterMonitor)) {
                        removeMonitorInstanceList.add(currentMonitor);
                        continue;
                    }

//                    如果相同则不用删除，也不用添加
                    if (afterMonitor.equals(currentMonitor)) {
                        addMonitorInstanceList.remove(afterMonitor);
                        continue;
                    }
//                    不相同则需要删除后再增加
                    removeMonitorInstanceList.add(currentMonitor);
                }
                break;
            default:
                break;
        }
        monitorNodeParams.setAddMonitorInstanceList(addMonitorInstanceList);
        monitorNodeParams.setRemoveMonitorInstanceList(removeMonitorInstanceList);

        return monitorNodeParams;
    }

    @Override
    public List<FlowDeployType> fitDeployType() {
        return Arrays.asList(FlowDeployType.MODIFY_MONITOR_OBJECT);
    }

    @Override
    public List<ResolvedEvent> resolvePipelineEventList(PipelineParameter pipelineParameter) throws Exception {
        return resolvePipelineEventList(pipelineParameter.getReq(), pipelineParameter.getFlowEntity());
    }


    private List<ResolvedEvent> resolvePipelineEventList(@Nullable DeployOneFlowReq req, ExecutionFlowEntity executionFlowEntity) throws Exception {
        PipelineFactory pipelineFactory = FactoryDiscoveryUtils.getFactoryByIdentifier(Constants.MONITOR_PIPELINE_FACTORY_IDENTIFY, PipelineFactory.class);
        PipelineParameter pipelineParameter = new PipelineParameter(req, executionFlowEntity, null, null);
        List<ResolvedEvent> resolvedEventList = pipelineFactory.analyzerAndResolveEvents(pipelineParameter);
        return resolvedEventList;
    }
}
