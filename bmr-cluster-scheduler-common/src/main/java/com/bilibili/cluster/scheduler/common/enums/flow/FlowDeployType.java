package com.bilibili.cluster.scheduler.common.enums.flow;

import lombok.Getter;

/**
 * @description: 工作流部署情况
 * @Date: 2024/5/9 16:59
 * @Author: nizhiqiang
 */
public enum FlowDeployType {

    CAPACITY_EXPANSION("扩容发布", true, "扩容发布", true, true, false, true, true),
    ITERATION_RELEASE("迭代发布", true, "迭代发布", true, true, false, true, true),
    ITERATION_RELEASE_LABEL("超配比", true, "超配比", true, true, false, true, true),
    // ON_LINE_DEPLOY("上线部署"),
//    等价于重启服务
    START_SERVICE("服务启动", false, "服务启动", true, true, false, true, true),
    //    等价于服务停止
    STOP_SERVICE("服务停止", false, "服务停止", true, true, false, true, true),
    RESTART_SERVICE("重启服务", false, "重启服务", true, true, false, true, true),
    OFF_LINE_EVICTION("下线驱逐", false, "下线驱逐", true, true, false, true, true),
    TIDE_ONLINE("潮汐上线", true, "潮汐上线", true, true, false, true, true),
    TIDE_OFFLINE("潮汐下线", false, "潮汐下线", true, true, false, true, true),
    K8S_CAPACITY_EXPANSION("K8s扩容发布", true, "K8s扩容发布", true, false, true, true, true),

    K8S_ITERATION_RELEASE("K8s迭代发布", true, "K8s迭代发布", true, false, true, true, true),

    MODIFY_MONITOR_OBJECT("监控对象变更", false, "监控对象变更", true, false, false, true, false),


    SPARK_EXPERIMENT("spark实验任务", true, "spark实验任务", true, false, false, false, false),
    SPARK_DEPLOY("spark版本发布", true, "spark版本发布", false, false, false, true, true),
    SPARK_DEPLOY_ROLLBACK("spark版本回滚", true, "spark版本回滚", false, false, false, true, true),


    SPARK_VERSION_LOCK("spark版本锁定", true, "spark版本锁定", true, false, false, true, true),
    SPARK_VERSION_RELEASE("spark版本解锁", true, "spark版本解锁", true, false, false, true, true),

    SPARK_CLIENT_PACKAGE_DEPLOY("spark客户端安装", true, "spark客户端安装", true, false, false, true, false),

    PRESTO_TIDE_OFF("presto潮汐下线", true, "presto潮汐下线,Yarn节点上线", true, false, true, true, true),
    PRESTO_TIDE_ON("presto潮汐上线", false, "presto潮汐上线,Yarn节点下线", true, false, true, true, true),

    CK_TIDE_OFF("clickhouse潮汐下线", true, "clickhouse潮汐下线,Yarn节点上线", true, false, true, true, true),
    CK_TIDE_ON("clickhouse潮汐上线", false, "clickhouse潮汐上线,Yarn节点下线", true, false, true, true, true),
    PRESTO_TO_PRESTO_TIDE_OFF("presto自动伸缩下线", false, "presto之间潮汐下线,Yarn节点上线", true, false, true, true, true),
    PRESTO_TO_PRESTO_TIDE_ON("presto自动伸缩上线", false, "presto之间潮汐上线,Yarn节点下线", true, false, true, true, true),

    HBO_JOB_PARAM_RULE_UPDATE("hbo的job param属性变更", true, "hob的job param属性变更", false, false, false, true, true),
    HBO_JOB_PARAM_RULE_DELETE("hbo的job param属性删除", true, "hob的job param属性删除", true, false, false, true, true),

    SPARK_PERIPHERY_COMPONENT_DEPLOY("Spark周边组件版本发布", true, "Spark周边组件版本发布", false, false, false, true, true),
    SPARK_PERIPHERY_COMPONENT_ROLLBACK("Spark周边组件版本回滚", true, "Spark周边组件版本回滚", false, false, false, true, true),
    SPARK_PERIPHERY_COMPONENT_LOCK("Spark周边组件版本锁定", true, "Spark周边组件版本锁定", false, false, false, true, true),
    SPARK_PERIPHERY_COMPONENT_RELEASE("Spark周边组件版本解锁", true, "Spark周边组件版本解锁", false, false, false, true, true),

    NNPROXY_DEPLOY("NNProxy服务发布", true, "NNProxy服务发布", false, false, false, true, true),
    NNPROXY_RESTART("NNProxy服务重启", true, "NNProxy服务重启", false, false, false, true, true),

    PRESTO_FAST_EXPANSION("presto快速扩容", true, "presto快速扩容", true, false, true, true, true),
    PRESTO_FAST_SHRINK("presto快速缩容", true, "presto快速缩容", true, false, true, true, true),

    YARN_TIDE_EXPANSION("Yarn潮汐扩容", true, "Yarn潮汐扩容", true, false, false, true, true),
    YARN_TIDE_SHRINK("Yarn潮汐缩容", true, "Yarn潮汐缩容", true, false, false, true, true),

    TRINO_EXPERIMENT("trino实验任务", true, "trino实验任务", true, false, false, false, false),
    ;

    @Getter
    String desc;

    //deploy是发布为true，启停为false
    @Getter
    Boolean type;

    // 发布的别名
    @Getter
    String dolphinAlias;

    @Getter
    boolean autoClose;

    //节点执行成功或者失败后是否同步到资源管理系统
    @Getter
    boolean refreshResource;

    //    是否容器发布
    @Getter
    boolean container;

    @Getter
    boolean failNotify;

    @Getter
    boolean isIncident;


    FlowDeployType(String desc, Boolean type, String alias, boolean autoClose, boolean refreshResource, boolean container, boolean failNotify, boolean isIncident) {
        this.desc = desc;
        this.type = type;
        this.dolphinAlias = alias;
        this.autoClose = autoClose;
        this.refreshResource = refreshResource;
        this.container = container;
        this.failNotify = failNotify;
        this.isIncident = isIncident;
    }

    public boolean isReleaseType() {
        return this.type;
    }

}
