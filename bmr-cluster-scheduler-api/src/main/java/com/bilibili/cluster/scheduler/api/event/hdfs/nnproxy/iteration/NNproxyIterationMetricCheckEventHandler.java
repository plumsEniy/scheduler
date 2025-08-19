package com.bilibili.cluster.scheduler.api.event.hdfs.nnproxy.iteration;

import cn.hutool.json.JSONUtil;
import com.bilibili.cluster.scheduler.api.event.AbstractTaskEventHandler;
import com.bilibili.cluster.scheduler.api.service.bmr.metadata.BmrMetadataService;
import com.bilibili.cluster.scheduler.api.service.flow.ExecutionNodePropsService;
import com.bilibili.cluster.scheduler.api.service.monitor.MonitorService;
import com.bilibili.cluster.scheduler.common.Constants;
import com.bilibili.cluster.scheduler.common.dto.bmr.metadata.MetadataComponentData;
import com.bilibili.cluster.scheduler.common.dto.hdfs.nnproxy.parms.NNProxyDeployFlowExtParams;
import com.bilibili.cluster.scheduler.common.dto.hdfs.nnproxy.parms.NNProxyDeployNodeExtParams;
import com.bilibili.cluster.scheduler.common.entity.ExecutionNodeEntity;
import com.bilibili.cluster.scheduler.common.enums.event.EventTypeEnum;
import com.bilibili.cluster.scheduler.common.enums.flow.FlowUrgencyType;
import com.bilibili.cluster.scheduler.common.event.TaskEvent;
import com.bilibili.cluster.scheduler.common.utils.DateTimeUtils;
import com.bilibili.cluster.scheduler.common.utils.LocalDateFormatterUtils;
import com.bilibili.cluster.scheduler.common.utils.RetryUtils;
import com.bilibili.cluster.scheduler.common.utils.ThreadUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @description:
 * @Date: 2025/4/28 20:00
 * @Author: nizhiqiang
 */

@Component
@Slf4j
public class NNproxyIterationMetricCheckEventHandler extends AbstractTaskEventHandler {

    @Resource
    ExecutionNodePropsService executionNodePropsService;

    @Resource
    BmrMetadataService bmrMetadataService;

    @Resource
    MonitorService monitorService;

    /**
     * 发布前后指标差异检查的map，默认为允许相差20%
     */
    private final Map<String, Integer> METRIC_GAP_MAP = new HashMap() {{
        put("RpcProcessingTimeNumOps", 20);
        put("DeleteAvgTime", 20);
        put("CreateAvgTime", 20);
        put("AddBlockAvgTime", 20);
        put("RenameAvgTime", 20);
        put("GetFileInfoAvgTime", 20);
    }};


    private final Map<String, Boolean> METRIC_REQUIRE_VALUE = new HashMap() {{
        put("RpcProcessingTimeNumOps", true);
        put("DeleteAvgTime", false);
        put("CreateAvgTime", false);
        put("AddBlockAvgTime", false);
        put("RenameAvgTime", false);
        put("GetFileInfoAvgTime", false);
    }};


    private static final String METRIC_TEMPLATE = "Hadoop_Router_%s{host=\"%s\",product=\"hdfs\"} or on() vector(0)";

    private static final String RPC_OPS_METRIC_TEMPLATE = "rate(Hadoop_Router_RpcProcessingTimeNumOps{host=\"%s\",product=\"hdfs\"}[2m]) or on() vector(0)";

    // https://info.bilibili.co/pages/viewpage.action?pageId=986265823
    // 支持额外的指标值检查
    private static final List<String> RATE_METRIC_KEY_LIST = Arrays.asList(
            "Hadoop_Router_RpcProcessingTimeNumOps",
            "Hadoop_Router_DeleteAvgTime",
            "Hadoop_Router_CreateAvgTime",
            "Hadoop_Router_AddBlockAvgTime",
            "Hadoop_Router_RenameAvgTime",
            "Hadoop_Router_GetFileInfoAvgTime"
            );
    private static final String NNPROXY_RATE_METRIC_TEMPLATE =
            "rate(%s{host=\"%s\",product=\"hdfs\"}[10m]) or on() vector(0)";

    @Override
    protected boolean skipLogicNode() {
        return true;
    }

    @Override
    protected boolean skipRollbackStatus() {
        return true;
    }

    private static String getMetricTemplate(String metricName, String hostName) {
        if (RATE_METRIC_KEY_LIST.contains(metricName)) {
            return String.format(NNPROXY_RATE_METRIC_TEMPLATE, metricName, hostName);
        }

        if (metricName.equals("RpcProcessingTimeNumOps")) {
            return String.format(RPC_OPS_METRIC_TEMPLATE, hostName);
        }
        return String.format(METRIC_TEMPLATE, metricName, hostName);
    }

    @Override
    public boolean executeTaskEvent(TaskEvent taskEvent) throws Exception {
        final Long flowId = taskEvent.getFlowId();
        final NNProxyDeployFlowExtParams flowExtParams = executionFlowPropsService.getFlowExtParamsByCache(flowId, NNProxyDeployFlowExtParams.class);
        final FlowUrgencyType urgencyType = flowExtParams.getUrgencyType();
        // 紧急发布跳过指标检查
        if (FlowUrgencyType.EMERGENCY.equals(urgencyType)) {
            logPersist(taskEvent, urgencyType.getDesc() + " 跳过指标检查。");
            return true;
        }

        ExecutionNodeEntity executionNode = taskEvent.getExecutionNode();
        Long executionNodeId = executionNode.getId();
        String hostname = executionNode.getNodeName();

        NNProxyDeployNodeExtParams executionNodeProps = executionNodePropsService.queryNodePropsByNodeId(executionNodeId, NNProxyDeployNodeExtParams.class);
        long componentId = executionNodeProps.getComponentId();

        MetadataComponentData componentData = bmrMetadataService.queryComponentByComponentId(componentId);
        String componentName = componentData.getComponentName();
        Map<String, String> componentVariableMap = bmrMetadataService.queryVariableByComponentId(componentId);
        HashMap<String, Integer> metricMap = new HashMap<>();
        metricMap.putAll(METRIC_GAP_MAP);

        boolean hasAdditionalCheck = false;
        for (Map.Entry<String, String> envEntry : componentVariableMap.entrySet()) {
            String envKey = envEntry.getKey().trim();
            String envValue = envEntry.getValue().trim();
            if (metricMap.containsKey(envKey)) {
                try {
                    if (envValue.endsWith("%")) {
                        envValue = envValue.replace("%", "");
                    }
                    int metricValue = Integer.parseInt(envValue);
                    if (metricValue <= 0) {
                        throw new IllegalArgumentException(String.format("%s下的nnproxy组件变量%s对应的值%s超出范围[0, ++&]",
                                componentName, envKey, metricValue));
                    }
                    metricMap.put(envKey, metricValue);
                } catch (Exception e) {
                    logPersist(taskEvent, String.format("%s下的nnproxy组件变量%s对应的值%s转换异常,改为默认20%指标差异检查,异常为%s",
                            componentName, envKey, envValue, e.getMessage()));
                }
            }

            if (RATE_METRIC_KEY_LIST.contains(envKey)) {
                try {
                    if (envValue.endsWith("ms")) {
                        envValue = envValue.replace("ms", "");
                    }
                    int metricValue = Integer.parseInt(envValue);
                    if (metricValue <= 0) {
                        throw new IllegalArgumentException(String.format("%s下的nnproxy组件变量%s对应的值%s超出范围[0, ++&]",
                                componentName, envKey, metricValue));
                    }
                    metricMap.put(envKey, metricValue);
                    hasAdditionalCheck = true;
                } catch (Exception e) {
                    logPersist(taskEvent, String.format("%s下的nnproxy组件变量%s对应的值%s转换异常,异常为%s",
                            componentName, envKey, envValue, e.getMessage()));
                }
            }
        }
        logPersist(taskEvent, "require check NNProxy metrics list is: " + JSONUtil.toJsonStr(metricMap.keySet()));

        // 若存在额外的指标检查，等待10分钟，否则，等待一分钟后检查指标值
        if (hasAdditionalCheck) {
            logPersist(taskEvent, "waiting 600s than check metrics....");
            ThreadUtils.sleep(Constants.ONE_MINUTES * 10);
        } else {
            logPersist(taskEvent, "waiting 60s than check metrics....");
            ThreadUtils.sleep(Constants.ONE_MINUTES * 1);
        }

        logPersist(taskEvent, "start check metrics....");

        LocalDateTime startTime = executionNodeProps.getStartTime();
        String startTimeFmt = LocalDateFormatterUtils.format(Constants.FMT_DATE_TIME, startTime);
        Long startStamp = DateTimeUtils.localDateTimeToTimestamp(startTime) / 1000;

        for (Map.Entry<String, Integer> entry : metricMap.entrySet()) {
            String metricName = entry.getKey();
            Integer metricGap = entry.getValue();

            LocalDateTime now = LocalDateTime.now();
            String nowTimeFmt = LocalDateFormatterUtils.format(Constants.FMT_DATE_TIME, now);
            Long nowStamp = DateTimeUtils.localDateTimeToTimestamp(now) / 1000;
            Double nowMetricValue = getMetricValue(hostname, nowStamp, metricName);
            logPersist(taskEvent, String.format("当前%s时间下的%s的指标为%s", nowTimeFmt, metricName, nowMetricValue));

            // 检查指标波动率
            if (METRIC_GAP_MAP.containsKey(metricName)) {
                logPersist(taskEvent, String.format("开始检查[%s]组件主机名称为[%s]的[%s]指标差异,指标允许最大波动为[%s]", componentName, hostname, metricName, metricGap + "%"));
                Double beforeMetricValue = getMetricValue(hostname, startStamp, metricName);
                logPersist(taskEvent, String.format("发布前%s时间下的%s的指标为%s", startTimeFmt, metricName, beforeMetricValue));

                boolean beforeHasValue = !beforeMetricValue.equals(0.0d);
                boolean currentHasValue = !nowMetricValue.equals(0.0d);

                if (beforeHasValue && currentHasValue) {
                    double gap = Math.abs(nowMetricValue - beforeMetricValue);
                    double gapPercent = gap * 1.0 / beforeMetricValue;
                    final DecimalFormat df = new DecimalFormat("#0.0");
                    final String gapPercentFmt = df.format(gapPercent * 100);

                    logPersist(taskEvent, String.format("发布前后%s指标差异为%s,波动为%s,允许最大波动为%s", metricName, gap, gapPercentFmt + "%", metricGap + "%"));
                    if (gapPercent > metricGap * 1.0 / 100) {
                        logPersist(taskEvent, String.format("指标%s波动过大，未通过检查", metricName));
                        return false;
                    }
                    logPersist(taskEvent, String.format("指标%s通过检查", metricName));
                } else if (METRIC_REQUIRE_VALUE.get(metricName)) {
                    if (!beforeHasValue) {
                        logPersist(taskEvent, String.format("发布前%s时间下的%s的必要指标为0,跳过检查", startTimeFmt, metricName));
                    }
                    if (!currentHasValue) {
                        logPersist(taskEvent, String.format("当前%s时间下的%s的必要指标为0,未通过检查", nowTimeFmt, metricName));
                        return false;
                    }
                } else {
                    if (!beforeHasValue) {
                        logPersist(taskEvent, String.format("发布前%s时间下的%s的指标为0,跳过检查", startTimeFmt, metricName));
                    }
                    if (!currentHasValue) {
                        logPersist(taskEvent, String.format("当前%s时间下的%s的指标为0,跳过检查", nowTimeFmt, metricName));
                    }
                }
            }

            // 检查指标绝对值大小
            if (RATE_METRIC_KEY_LIST.contains(metricName)) {
                if (nowMetricValue > metricGap * 1.0) {
                    logPersist(taskEvent, String.format("当前%s时间下的%s的指标为%s, 超过配置的允许最大值%s, 检查未通过", startTimeFmt, metricName, nowMetricValue, metricGap));
                    return false;
                } else {
                    logPersist(taskEvent, String.format("当前%s时间下的%s的指标为%s, 小于配置的允许最大值%s, 检查通过", startTimeFmt, metricName, nowMetricValue, metricGap));
                }
            }
        }
        return true;
    }

    private Double getMetricValue(String hostname, Long startStamp, String metricName) throws Exception {
       return RetryUtils.retryWith(3, 5, () -> {
            Double beforeMonitorValue = Double.valueOf(monitorService.queryMonitor(
                    getMetricTemplate(metricName, hostname), startStamp)
                    .getValue());
            return beforeMonitorValue;
        });
    }

    @Override
    public EventTypeEnum getHandleEventType() {
        return EventTypeEnum.NN_PROXY_ITERATION_METRICS_CHECK;
    }
}
