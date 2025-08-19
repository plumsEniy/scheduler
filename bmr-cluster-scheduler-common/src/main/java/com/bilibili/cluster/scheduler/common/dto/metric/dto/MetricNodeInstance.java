package com.bilibili.cluster.scheduler.common.dto.metric.dto;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 采集任务信息
 */
@Data
public class MetricNodeInstance {
    // hostname
    private String name;
    // ip
    private String target;
    // port
    private Integer port;
    // service
    private String type;
    /**
     * 标签目前需要填写 app,zone,env,namespace,label,host,cluster
     * 平台会自动填充product，对比的时候需要忽略product属性
     */
    private Map<String, String> labels;
    private Boolean enable_scrape = true;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MetricNodeInstance that = (MetricNodeInstance) o;

        if (!Objects.equals(name, that.name)) return false;
        if (!Objects.equals(target, that.target)) return false;
        if (!Objects.equals(port, that.port)) return false;
        if (!Objects.equals(type, that.type)) return false;

        Map<String, String> labels1Map = this.getLabels();
        Map<String, String> labels2Map = that.getLabels();

        if (!compareMapsIgnoringKey(labels1Map, labels2Map, "product")) {
            return false;
        }

        return Objects.equals(enable_scrape, that.enable_scrape);
    }

    public static boolean compareMapsIgnoringKey(Map<?, ?> map1, Map<?, ?> map2, Object keyToIgnore) {
        // 检查两个 Map 是否相等，除了忽略的键
        for (Map.Entry<?, ?> entry : map1.entrySet()) {
            if (entry.getKey().equals(keyToIgnore)) {
                continue; // 忽略指定的键
            }
            if (!map2.containsKey(entry.getKey()) || !map2.get(entry.getKey()).equals(entry.getValue())) {
                return false; // 找到不匹配的键值对
            }
        }

        // 检查 map2 中的键，如果忽略的键在 map1 中，跳过
        for (Map.Entry<?, ?> entry : map2.entrySet()) {
            if (entry.getKey().equals(keyToIgnore)) {
                continue; // 忽略指定的键
            }
            // 这里已经确保 map1 中的键已经检查过，因此只需检查 map2 中的额外键
            if (!map1.containsKey(entry.getKey())) {
                return false; // map2 中有 map1 没有的键
            }
        }

        return true; // 所有其他键均匹配
    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (target != null ? target.hashCode() : 0);
        result = 31 * result + (port != null ? port.hashCode() : 0);
        result = 31 * result + (type != null ? type.hashCode() : 0);

        HashMap<String, String> labelMap = new HashMap<>();
        labelMap.putAll(labels);
        labelMap.remove("product");
        result = 31 * result + (labelMap != null ? labelMap.hashCode() : 0);
        result = 31 * result + (enable_scrape != null ? enable_scrape.hashCode() : 0);
        return result;
    }
}
