package com.bilibili.cluster.scheduler.common.utils;

import com.bilibili.cluster.scheduler.common.dto.bmr.resourceV2.model.ComponentHostRelationModel;
import com.bilibili.cluster.scheduler.common.dto.metric.dto.MetricConfInfo;
import com.bilibili.cluster.scheduler.common.dto.metric.dto.MetricNodeInstance;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MonitorConfUtil {

    /**
     * 根据主机relation列表和监控属性生产监控对象列表
     *
     * @param hostRelationList
     * @param monitor
     * @return
     */
    public static List<MetricNodeInstance> getNodeInstanceList(List<ComponentHostRelationModel> hostRelationList, MetricConfInfo monitor) {

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

}
