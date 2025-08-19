package com.bilibili.cluster.scheduler.common.dto.monitor.dto;

import lombok.Data;

import java.util.List;

/**
 * @description: 监控属性
 * @Date: 2025/4/27 17:50
 * @Author: nizhiqiang
 */
@Data
public class MonitorValue {

    private Double stamp;

    private String value;

    public static MonitorValue generateMonitorValue(List<Object> valueList) {
        MonitorValue monitorValue = new MonitorValue();
        Double stamp = Double.valueOf(String.valueOf(valueList.get(0)));
        monitorValue.setStamp(stamp);

        String value = (String) valueList.get(1);
        monitorValue.setValue(value);
        return monitorValue;
    }
}
