import cn.hutool.json.JSONUtil;
import com.bilibili.cluster.scheduler.api.ApiApplicationServer;
import com.bilibili.cluster.scheduler.api.service.bmr.resourceV2.BmrResourceV2Service;
import com.bilibili.cluster.scheduler.api.service.flow.prepare.factory.monitor.MonitorDeployFlowPrepareGenerateFactory;
import com.bilibili.cluster.scheduler.api.service.metric.MetricService;
import com.bilibili.cluster.scheduler.common.Constants;
import com.bilibili.cluster.scheduler.common.dto.bmr.resourceV2.model.ComponentHostRelationModel;
import com.bilibili.cluster.scheduler.common.dto.bmr.resourceV2.req.QueryComponentHostPageReq;
import com.bilibili.cluster.scheduler.common.dto.metric.dto.*;
import com.bilibili.cluster.scheduler.common.dto.parameters.dto.flow.metric.MetricExtParams;
import com.bilibili.cluster.scheduler.common.dto.parameters.dto.node.monitor.MonitorNodeParams;
import com.bilibili.cluster.scheduler.common.entity.ExecutionFlowEntity;
import com.bilibili.cluster.scheduler.common.entity.ExecutionNodeEntity;
import com.bilibili.cluster.scheduler.common.enums.NodeExecuteStatusEnum;
import com.bilibili.cluster.scheduler.common.enums.metric.MetricEnvEnum;
import com.bilibili.cluster.scheduler.common.enums.metric.MetricModifyType;
import com.bilibili.cluster.scheduler.common.enums.node.NodeOperationResult;
import com.bilibili.cluster.scheduler.common.utils.ComponentUtils;
import lombok.extern.slf4j.Slf4j;
import org.asynchttpclient.AsyncCompletionHandler;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.asynchttpclient.Response;
import org.asynchttpclient.request.body.multipart.Part;
import org.asynchttpclient.request.body.multipart.StringPart;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StopWatch;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.Future;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @description: 监控查询
 * @Date: 2024/8/30 15:13
 * @Author: nizhiqiang
 */

@RunWith(SpringRunner.class)
@SpringBootTest(classes = ApiApplicationServer.class)
@Slf4j
public class MonitorTest {

    @Resource
    MetricService metricService;

//    @Resource
//    MonitorConfigUtil monitorConfigUtil;

    @Resource
    BmrResourceV2Service bmrResourceV2Service;

    private static String token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJleHAiOjIwMjg1MTIyNjQsImlhdCI6MTcxMzE1MjI2NCwiaXNzIjoibml6aGlxaWFuZyIsIlBsYXRmb3JtTmFtZSI6ImJtci3mjIfmoIfnm5HmjqciLCJFeHBpcmVBdCI6IjAwMDEtMDEtMDFUMDA6MDA6MDBaIn0.wwyHNv04W0Oh3m9CjS6BHzrX9eLKFUMaq-mVEeSbqBM";

    @Resource
    MonitorDeployFlowPrepareGenerateFactory monitorDeployFlowPrepareGenerateFactory;

//    @Test
//    public void testMonitorMap() {
//        MonitorTokenConfig dataNode = monitorConfigUtil.getMonitorConfigByComponentName("DataNode", MonitorEnvEnum.PROD);
//        System.out.println("dataNode = " + dataNode);
//    }

    @Test
    public void testQueryMonitorInstanceList() {
        List<MetricNodeInstance> metricNodeInstanceList = metricService.queryMetricInstanceList(MetricEnvEnum.PROD, 76);
        System.out.println("monitor node list size is " + metricNodeInstanceList.size());
    }

    @Test
    public void testGenerateNodeAndEvents() throws Exception {

        ExecutionFlowEntity executionFlowEntity = new ExecutionFlowEntity();
        executionFlowEntity.setId(10145L);
        monitorDeployFlowPrepareGenerateFactory.generateNodeAndEvents(executionFlowEntity);

    }

    @Test
    public void testAddMonitorInstanceList() {
        String token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJleHAiOjIwMTg1MDQ5MzEsImlhdCI6MTcwMzE0NDkzMSwiaXNzIjoibml6aGlxaWFuZyIsIlBsYXRmb3JtTmFtZSI6InByZeeOr-Wig2Jtci3mjIfmoIfnm5HmjqciLCJFeHBpcmVBdCI6IjAwMDEtMDEtMDFUMDA6MDA6MDBaIn0.VkU7x8VpedcoQJDeAIhMASnd_vJMMC0kwAkZgABs3Ug";
        UpdateMetricDto updateMetricDto = new UpdateMetricDto();
        updateMetricDto.setIntegrationId(30);

        List<MetricNodeInstance> metricNodeInstanceList = new ArrayList<>();
        MetricNodeInstance metricNodeInstance = new MetricNodeInstance();
        metricNodeInstance.setPort(1234);
        metricNodeInstance.setName("7031");
        metricNodeInstance.setTarget("10.157.9.16");
        HashMap<String, String> labelMap = new HashMap<>();
        labelMap.put("zone", "sh001");
        labelMap.put("label", "NaN");
        labelMap.put("env", "prod");
        labelMap.put("cluster", "test");
        metricNodeInstance.setLabels(labelMap);
        metricNodeInstanceList.add(metricNodeInstance);
        updateMetricDto.setInstances(metricNodeInstanceList);

//        monitorService.addMonitorInstance(MonitorEnvEnum.PROD, updateMonitorDto, token);
//        monitorService.addMonitorInstance(MonitorEnvEnum.PROD, updateMonitorDto, token);

        metricService.delMetricInstance(MetricEnvEnum.PROD, updateMetricDto, token);
        metricService.delMetricInstance(MetricEnvEnum.PROD, updateMetricDto, token);
    }

    public static void main(String[] args) {

        List<Part> parts = new ArrayList<>();
        parts.add(new StringPart("id", "82", "multipart/form-data"));

        final String response = asyncRequest("http://cloud.bilibili.co/metrics/api/v1/open/instances/list",
                parts);

        System.out.println(response);
    }

    public static String asyncRequest(String url, List<Part> params) {
        AsyncHttpClient c = new DefaultAsyncHttpClient();
        Future<Response> f = c.prepareGet(url).setBodyParts(
                        params)
                // .addHeader("authorization",TokenManager.getToken())
                .execute(new AsyncCompletionHandler<Response>() {

                    @Override
                    public Response onCompleted(Response response) {
                        return response;
                    }

                    @Override
                    public void onThrowable(Throwable t) {
                        throw new RuntimeException("exception");
                    }
                });
        Response response = null;
        try {
            long startTime = System.currentTimeMillis();
            while (true) {
                response = f.get();
                if (response != null) {
                    break;
                } else {
                    long endTime = System.currentTimeMillis();
                    // 大于20秒认为查询数据失败
                    if ((endTime - startTime) / 1000 > 40) {
                        throw new RuntimeException("exception");
                    }
                }
            }
        } catch (Exception e) {
            log.error("数据异常");
        }
        return response == null ? "" : response.getResponseBody();
    }

    /**
     * 根据主机relation列表和监控属性生产监控对象列表
     *
     * @param hostRelationList
     * @param monitor
     * @return
     */
    private List<MetricNodeInstance> getNodeInstanceList(List<ComponentHostRelationModel> hostRelationList, MetricConfInfo monitor) {

        //获取label
        List<MetricNodeInstance> nodeInstanceList = new ArrayList<>();

        String componentNameAlias = monitor.getComponentNameAlias();
        String nameSpace = "";
        if (ComponentUtils.isNameNode(componentNameAlias) || ComponentUtils.isNnProxy(componentNameAlias)) {
            nameSpace = componentNameAlias;
        }
        String componentName = monitor.getComponentName();
        String[] ports = monitor.getPorts().split(",");
        for (ComponentHostRelationModel hostRelation : hostRelationList) {
            for (String port : ports) {
                MetricNodeInstance nodeInstance = new MetricNodeInstance();
                nodeInstance.setType(monitor.getMonitorObjectType());
                Map<String, String> labelMap = new HashMap<>();
                labelMap.put("app", monitor.getAppId());
                labelMap.put("zone", hostRelation.getZone());
                labelMap.put("env", monitor.getEnvType().name().toLowerCase());
                labelMap.put("namespace", nameSpace);
                switch (componentName) {
                    case "NodeManager":
                    case "ResourceManager":
                    case "Amiya":
                    case "SparkEssWorker":
                    case "SparkEssMaster":
                        String label = hostRelation.getLabelName();
                        labelMap.put("label", label);
                        break;
                }
                labelMap.put("host", hostRelation.getHostName());
                labelMap.put("cluster", monitor.getClusterAlias());

                nodeInstance.setName(hostRelation.getHostName());
                nodeInstance.setTarget(hostRelation.getIp());
                nodeInstance.setLabels(labelMap);
                nodeInstance.setPort(Integer.parseInt(port));

                nodeInstanceList.add(nodeInstance);
            }
        }
        return nodeInstanceList;
    }

    @Test
    public void testDiffFlinkMonitor() {

        LocalDateTime startTime = LocalDateTime.now();
        MetricExtParams metricExtParams = new MetricExtParams();
        metricExtParams.setModifyType(MetricModifyType.CRON_SYNC_JOB);
        metricExtParams.setComponentAliasName("Flink-k8s");
        metricExtParams.setEnvType(MetricEnvEnum.PROD);

        MetricConfInfo before = new MetricConfInfo();
        before.setPorts("9250,9251,9252,9253,9254,9255,9256,9257,9258,9259,9260,9261,9262,9263,9264,9265,9266,9267,9268,9269,9270,9271,9272,9273,9274,9275,9276,9277,9278,9279,9280,9281,9282,9283,9284,9285,9286,9287,9288,9289,9290,9291,9292,9293,9294,9295,9296,9297,9298,9299,9300,9301,9302,9303,9304,9305,9306,9307,9308,9309,9310,9311,9312,9313,9314,9315,9316,9317,9318,9319,9320,9321,9322,9323,9324,9325,9326,9327,9328,9329,9330,9331,9332,9333,9334,9335,9336,9337,9338,9339,9340,9341,9342,9343,9344,9345,9346,9347,9348,9349,9350,9351,9352,9353,9354,9355,9356,9357,9358,9359,9360");
        before.setComponentId(5L);

        MetricConfInfo after = new MetricConfInfo();
        after.setPorts("9250,12345");
        after.setComponentId(5L);
        after.setComponentNameAlias("Flink-k8s");
        after.setComponentName("Flink-k8s");
        after.setClusterId(2L);
        after.setClusterAlias("flink_native_on_k8s");
        after.setAppId("flink_exporter");
        after.setMonitorObjectType("servers");
        after.setPath("NaN");
        after.setEnvType(MetricEnvEnum.PROD);

        metricExtParams.setBefore(before);
        metricExtParams.setIntegrationId(26);
        metricExtParams.setAfterList(Arrays.asList(new MetricConfInfo[]{after}));
        MetricEnvEnum envType = metricExtParams.getEnvType();
        Integer integrationId = metricExtParams.getIntegrationId();

        log.info("pararms {}", JSONUtil.toJsonStr(metricExtParams));

        StopWatch sw = new StopWatch();
        sw.start("查询主机列表");

        //            主机名和host相关的map
        HashMap<String, MetricHostInfo> hostNameToHostInfoMap = new HashMap<>();
//            所有更新后的监控实例列表
        List<MetricNodeInstance> afterMonitorInstanceList = new ArrayList<>();
        QueryComponentHostPageReq req = new QueryComponentHostPageReq();
        req.setComponentId(after.getComponentId());
        req.setClusterId(after.getClusterId());
        List<ComponentHostRelationModel> componentHostRelationList = bmrResourceV2Service.queryComponentHostList(req);
        afterMonitorInstanceList.addAll(getNodeInstanceList(componentHostRelationList, after));
        componentHostRelationList.forEach(hostRelation -> {
            hostNameToHostInfoMap.putIfAbsent(hostRelation.getHostName(), new MetricHostInfo(hostRelation.getHostName(), hostRelation.getIp(), hostRelation.getRack()));
        });

        sw.stop();

        sw.start("查询监控列表");
        Map<String, List<MetricNodeInstance>> hostNameToAfterMonitorInstancesMap = afterMonitorInstanceList.stream().collect(Collectors.groupingBy(MetricNodeInstance::getName));
        List<MetricNodeInstance> metricNodeInstanceList = metricService.queryMetricInstanceList(envType, integrationId);
        sw.stop();

        sw.start("监控diff对比");
        Map<String, List<MetricNodeInstance>> hostNameToCurrentMonitorInstanceMap = metricNodeInstanceList.stream().collect(Collectors.groupingBy(MetricNodeInstance::getName));

        Map<String, MonitorNodeParams> hostNameToParamsMap = generateMonitorNodeParams(metricExtParams, integrationId, hostNameToAfterMonitorInstancesMap, hostNameToCurrentMonitorInstanceMap);

        log.info("end init ");

        sw.stop();
        log.info(sw.prettyPrint());

        int batchId = 1;
        int currs = 0;
        List<ExecutionNodeEntity> executionNodeList = new ArrayList<>();
        Integer flowParallelism = 1;
        for (String hostName : hostNameToParamsMap.keySet()) {
            if (!CollectionUtils.isEmpty(hostNameToParamsMap.get(hostName).getAddMonitorInstanceList())) {
                log.info("host name is {}", hostName);
            }
            MetricHostInfo hostInfo = hostNameToHostInfoMap.get(hostName);
//                删除已经作废的节点时，可能无法在组件中查到节点信息
            String ip = Optional.ofNullable(hostInfo).map(MetricHostInfo::getIp).orElse(Constants.EMPTY_STRING);
            String rack = Optional.ofNullable(hostInfo).map(MetricHostInfo::getRack).orElse(Constants.EMPTY_STRING);

            ExecutionNodeEntity executionNodeEntity = new ExecutionNodeEntity();
            executionNodeEntity.setNodeName(hostName);
            executionNodeEntity.setBatchId(batchId);
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

        log.info("start time {}, current time {}", startTime, LocalDateTime.now());
    }

    @NotNull
    private Map<String, MonitorNodeParams> generateMonitorNodeParams(MetricExtParams metricExtParams, Integer integrationId, Map<String, List<MetricNodeInstance>> hostNameToAfterMonitorInstancesMap, Map<String, List<MetricNodeInstance>> hostNameToCurrentMonitorInstanceMap) {
        Map<String, MonitorNodeParams> hostNameToParamsMap = new HashMap<>();
        MetricModifyType modifyType = metricExtParams.getModifyType();

        //            全量修改的时候，需要删除修改前不存在的主机
        if (MetricModifyType.CRON_SYNC_JOB.equals(modifyType)) {
            for (Map.Entry<String, List<MetricNodeInstance>> entry : hostNameToCurrentMonitorInstanceMap.entrySet()) {
                String hostName = entry.getKey();
                List<MetricNodeInstance> monitorInstanceList = entry.getValue();
                if (!hostNameToAfterMonitorInstancesMap.containsKey(hostName)) {
                    MonitorNodeParams monitorNodeParams = new MonitorNodeParams();
                    monitorNodeParams.setToken(monitorNodeParams.getToken());
                    monitorNodeParams.setIntegrationId(integrationId);
                    monitorNodeParams.setMonitorEnv(metricExtParams.getEnvType());
                    monitorNodeParams.setRemoveMonitorInstanceList(monitorInstanceList);
                    hostNameToParamsMap.put(hostName, monitorNodeParams);
                    System.out.println("monitorNodeParams = " + JSONUtil.toJsonStr(monitorNodeParams));
                    log.info("monitor remove node size {}", monitorNodeParams.getRemoveMonitorInstanceList().size());
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
            nodeAfterMonitorInstanceList.stream().collect(Collectors.toMap(MetricNodeInstance::getPort, Function.identity()));
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
}
