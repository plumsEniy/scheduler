package com.bilibili.cluster.scheduler.common.dto.bmr.resource.req;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * @description: 查询主机
 * @Date: 2024/5/23 09:02
 * @Author: nizhiqiang
 */

@Data
public class QueryHostInfoReq {
    /**
     * 主机名列表
     */
    private List<String> hostNameList;

    /**
     * 主机名
     */
    private String hostName;

    /**
     * 组件ID
     */
    private Long componentId;

    /**
     * 组件名称
     */
    private String componentName;
    /**
     * 发布类型
     */
    private String deployType;

    /**
     * 集群ID
     */
    private Long clusterId;

    /**
     * 主机初始化状态
     */
    private String initState;

    // 当前部署组件
    private List<String> componentList = new ArrayList<>();
    // 计划部署组件
    private List<String> planComponentList = new ArrayList<>();
    // 是否比较不同
    private Boolean isDiff;

    private int pageNum;
    private int pageSize;
}
