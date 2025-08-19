package com.bilibili.cluster.scheduler.common.utils;

/**
 * @description: 组件名工具
 * @Date: 2024/5/10 19:01
 * @Author: nizhiqiang
 */
public class ComponentUtils {
    public static boolean isNameNode(String componentName) {
        return componentName.startsWith("jscs-bigdata-ec") || componentName.startsWith("jscs-bigdata-ns");
    }

    public static boolean isNnProxy(String componentName) {
        return componentName.contains("proxy-ns");
    }

    public static boolean isDataNode(String componentName){
        return componentName.equals("DataNode");
    }
}
