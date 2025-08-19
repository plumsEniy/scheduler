package com.bilibili.cluster.scheduler.common.enums.dataNode;

import com.bilibili.cluster.scheduler.common.Constants;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.util.StringUtils;

import java.util.Map;

/**
 * @description: datanode的版本
 * @Date: 2024/5/10 18:55
 * @Author: nizhiqiang
 */
@AllArgsConstructor
public enum DataNodeVersionEnum {

    VERSION2("2.8版本"),
    VERSION3("3.3版本"),
    ;

    @Getter
    private String desc;

    public static DataNodeVersionEnum getNodeVersionByVariableMap(Map<String, String> variableMap, long componentId) {
        String dnVersion = variableMap.get(Constants.HADOOP_DN_VERSION);
        if (StringUtils.isEmpty(dnVersion)) {
            throw new IllegalArgumentException(String.format("datanode节点未配置组件变量:%s,组件id为%s", Constants.HADOOP_DN_VERSION, componentId));
        }

        switch (dnVersion) {
            case "2":
                return VERSION2;
            case "3":
                return VERSION3;
            default:
                throw new IllegalArgumentException(String.format("datanode组件变量配置错误,变量名:%s,变量为%s,组件id为%s"
                        , Constants.HADOOP_DN_VERSION, dnVersion, componentId));
        }

    }
}
