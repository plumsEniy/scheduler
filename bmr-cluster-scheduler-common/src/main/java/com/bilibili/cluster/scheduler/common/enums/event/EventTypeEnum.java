package com.bilibili.cluster.scheduler.common.enums.event;


import lombok.Getter;

/***
 *
 * */
@Getter
public enum EventTypeEnum {

    /**
     * dn下线
     * 1、开始fastdecommission
     * 2、主机加入到配置文件（2.8的namenode修改fastdecommission增加主机，3.3的maintenance加上主机状态为fastdecommission）
     * 3、修改balalcecer文件并上传到boss（主机加入balalcer的excludes文件）
     * 4、检查namenode并更新节点信息
     * 5、检查fastdecmission是否完成，超过12天则跳过
     * 6、主机修改配置文件（2.8的namenode修改exclude文件增加主机，3.3的maintenance的fastdecommission状态修改成decomission）
     * 7、检查namenode并更新节点信息
     * 8、检查decommission的接口，超过5小时则跳过
     * 9、检查nn的指标数量是否小于10
     * 10、主机修改配置文件（2.8的namenode修改exclude，includes文件移除主机，3.3的maintenance的fastdecommission）
     * 11、修改balalcecer文件并上传到boss（主机移除balalcer的excludes文件）
     * 12、检查namenode并更新节点信息
     * 13、刷新机架文件（移除）
     */
    DN_EVICT_START_FASTDECOMISSION("执行Fast Decomission", true, false),
    DN_EVICT_ADD_FASTDECOMISSION_V2("添加fastdecommission文件", true, false),
    DN_EVICT_ADD_FASTDECOMISSION_V3("添加maintenance文件", true, false),
    DN_EVICT_REFRESH_NAMENODE("刷新NameNode", true, false),
    DN_EVICT_CHECK_FASTDECOMISSION("检查Fast Decommission执行结果", true, false),
    DN_EVICT_UPDATE_DECOMISSION_V2("修改Excludes和fastdecommission文件", true, false),
    DN_EVICT_UPDATE_DECOMISSION_V3("修改maintenance文件节点状态", true, false),
    DN_EVICT_CHECK_DECOMISSION("检查decommission结果", true, false),
    DN_EVICT_CHECK_DATA_MIGRATION("检查数据迁移结果", true, false),
    DN_EVICT_REMOVE_DECOMISSION_V2("移除DN下线节点", true, false),
    DN_EVICT_REMOVE_DECOMISSION_V3("移除DN下线节点", true, false),
    DN_EVICT_DN_STOP("停止DN服务", true, false),

    /**
     * dolphinScheduler流程执行event
     */
    DOLPHIN_SCHEDULER_PIPE_EXEC_EVENT("dolphinScheduler流程执行", true, true),

    REFRESH_RESOURCE_MANAGER_INFO_EVENT("刷新资源节点", true, false),

    RANDOM_TEST_EXEC_EVENT("随机测试节点", true, false),

    BATCH_ALIGN_TEST_EXEC_EVENT("批次对齐节点", true, false),

    MONITOR_OBJECT_CHANGE_EXEC_EVENT("监控节点变更", true, false),

    MONITOR_WITH_DEPLOY_EXEC_EVENT("部署节点监控变更", true, false),
    @Deprecated
    FLINK_NM_DELETE_MONITOR_OBJECT_CHANGE_EXEC_EVENT("flink-nm监控节点移除", true, false),

    /**
     * spark experiment
     */
    SPARK_EXPERIMENT_CREATE_EXEC_EVENT("创建spark实验", false, false),
    SPARK_EXPERIMENT_QUERY_EXEC_EVENT("查询实验结果", false, false),

    SPARK_VERSION_DEPLOY_PRE_CHECK("任务检查", true, false),
    SPARK_VERSION_DEPLOY_DQC("数据质量验证", true, false),
    SPARK_VERSION_DEPLOY_EXEC_EVENT("版本变更", true, false),
    SPARK_VERSION_DEPLOY_POST_CHECK("后置检查", true, false),
    SPARK_VERSION_DEPLOY_STAGE_CHECK("阶段检查", true, false),

    SPARK_VERSION_STAGE_CHECK_EXEC_EVENT("spark版本阶段间检查", true, false),

    SPARK_VERSION_LOCK_EXEC_EVENT("版本锁定", true, false),
    SPARK_VERSION_RELEASE_EXEC_EVENT("版本解锁", true, false),

    SPARK_CLIENT_PACK_DOWNLOAD_EVENT("客户端下发", true, true),
    SPARK_CLIENT_PACK_REMOVE_EVENT("客户端移除", true, true),
    SPARK_CLIENT_PACK_CLEAN_EVENT("客户端清理", true, true),

    // presto潮汐下线
    // stage 1
    PRESTO_TIDE_OFF_POD_FAST_SHRINKAGE("presto节点快速缩容", true, false),
    PRESTO_TIDE_OFF_WAIT_AVAILABLE_NODES("等待presto缩容节点可用", true, false),
    // stage 2
    PRESTO_TIDE_OFF_EXPANSION_YARN_NODES("presto的yarn计算节点扩容", true, true),
    PRESTO_TIDE_OFF_UPDATE_NODE_SERVICE_STATE("潮汐下线节点状态更新", true, false),

    // presto潮汐上线
    // stage 1
    // PRESTO_TIDE_ON_CAPTURE_YARN_NODES("待缩容yarn节点确认", true, false), prepare do it
    PRESTO_TIDE_ON_WAIT_APP_GRACEFUL_FINISH("等待任务优雅退出", true, false),
    PRESTO_TIDE_ON_EVICTION_YARN_NODES("下线yarn节点", true, true),
    PRESTO_TIDE_ON_UPDATE_NODE_SERVICE_STATE("潮汐上线节点状态更新", true, false),
    // stage 2
    PRESTO_TIDE_ON_POD_EXPANSION("presto节点扩容", true, false),
    PRESTO_TIDE_ON_POD_STATUS_CHECK("presto扩容节点状态检查", true, false),

    // presto to presto潮汐下线
    // stage 1
    PRESTO_TO_PRESTO_TIDE_OFF_POD_FAST_SHRINKAGE("presto节点快速缩容", true, false),
    PRESTO_TO_PRESTO_TIDE_OFF_WAIT_SHRINKAGE_POD("等待presto缩容节点可用", true, false),
    PRESTO_TO_PRESTO_TIDE_OFF_POD_EXPANSION("presto节点扩容", true, false),
    PRESTO_TO_PRESTO_TIDE_OFF_WAIT_EXPANSION_POD("等待presto扩容节点可用", true, false),

    // presto to presto潮汐上线
    // stage 2
    PRESTO_TO_PRESTO_TIDE_ON_POD_FAST_SHRINKAGE("presto节点快速缩容", true, false),
    PRESTO_TO_PRESTO_TIDE_ON_WAIT_SHRINKAGE_POD("等待presto缩容节点可用", true, false),
    PRESTO_TO_PRESTO_TIDE_ON_POD_EXPANSION("presto节点扩容", true, false),
    PRESTO_TO_PRESTO_TIDE_ON_WAIT_EXPANSION_POD("等待presto扩容节点可用", true, false),

    // presto快速扩缩容
    PRESTO_POD_FAST_SHRINK("presto节点快速缩容", true, false),
    PRESTO_POD_SHRINKAGE_WAIT_READY("presto节点等待缩容完毕", true, false),
    PRESTO_POD_FAST_EXPANSION("presto节点快速扩容", true, false),
    PRESTO_POD_EXPANSION_WAIT_READY("presto节点等待扩容完毕", true, false),

    // yarn集群节点潮汐
    //    扩容
    PRESTO_YARN_TIDE_EXPANSION_PRE_CHECK("扩容预校验", true, false),
    PRESTO_YARN_TIDE_EXPANSION_WAITING_AVAILABLE_NODES("等待节点可用", true, false),
    //    PRESTO_TIDE_OFF_EXPANSION_YARN_NODES("presto的yarn计算节点扩容", true, true),
    //    PRESTO_TIDE_OFF_UPDATE_NODE_SERVICE_STATE("潮汐下线节点状态更新", true, false),
    // ---------------------------------------------------------------------------------------------
    //    缩容（复用老的流程）
    //    PRESTO_TIDE_ON_WAIT_APP_GRACEFUL_FINISH("等待任务优雅退出", true, false),
    //    PRESTO_TIDE_ON_EVICTION_YARN_NODES("下线yarn节点", true, true),
    //    PRESTO_TIDE_ON_UPDATE_NODE_SERVICE_STATE("潮汐上线节点状态更新", true, false),


    // ck潮汐下线
    // stage 1
    CK_TIDE_OFF_POD_FAST_SHRINKAGE("ck节点快速缩容", true, false),
    CK_TIDE_OFF_WAIT_AVAILABLE_NODES("等待ck缩容节点可用", true, false),
    CK_TIDE_OFF_KILL_PVC("删除pvc", true, false),

    // stage 2
    CK_TIDE_OFF_EXPANSION_YARN_NODES("ck的yarn计算节点扩容", true, true),
    CK_TIDE_OFF_UPDATE_NODE_SERVICE_STATE("潮汐下线节点状态更新", true, false),

    // CK潮汐上线
    // stage 1
    CK_TIDE_ON_WAIT_APP_GRACEFUL_FINISH("等待任务优雅退出", true, false),
    CK_TIDE_ON_EVICTION_YARN_NODES("下线yarn节点", true, true),
    CK_TIDE_ON_UPDATE_NODE_SERVICE_STATE("潮汐上线节点状态更新", true, false),
    // stage 2
    CK_TIDE_ON_POD_EXPANSION("ck节点扩容", true, false),
    CK_TIDE_ON_POD_STATUS_CHECK("ck扩容节点状态检查", true, false),

    /**
     * hbo job params
     */
    HBO_JOB_PARAMS_UPDATE("hbojob参数变更", true, false),
    HBO_JOB_PARAMS_DELETE("hbojob任务删除", true, false),

    /**
     * CK容器发布
     */
    CK_CONTAINER_DEPLOY("ck容器发布", true, false),
    CK_CHECK_CONTAINER("检查容器状态", true, false),

    /**
     * spark周边组件版本发布
     */
    SPARK_PERIPHERY_COMPONENT_DEPLOY_PRE_CHECK("任务检查", true, false),
    SPARK_PERIPHERY_COMPONENT_DEPLOY_UPDATE_VERSION("版本变更", true, false),
    SPARK_PERIPHERY_COMPONENT_DEPLOY_STAGE_CHECK("阶段检查", true, false),

    /**
     * spark周边组件版本锁定
     */
    SPARK_PERIPHERY_COMPONENT_LOCK_PRE_CHECK("任务检查", true, false),
    SPARK_PERIPHERY_COMPONENT_LOCK_VERSION_UPDATE("版本锁定状态变更", true, false),
    SPARK_PERIPHERY_COMPONENT_LOCK_STAGE_CHECK("阶段检查", true, false),

    /**
     * NNProxy发布
     */
    NN_PROXY_EXPANSION_PIPELINE_EVENT("NNProxy扩容", true, true),
    NN_PROXY_EXPANSION_UPDATE_STATE("NNProxy状态更新", true, false),

    NN_PROXY_ITERATION_PRE_CHECK("NNProxy预检查", true, false),
    NN_PROXY_ITERATION_PIPELINE_EVENT("NNProxy迭代", true, true),
    NN_PROXY_ITERATION_METRICS_CHECK("NNProxy指标检查", true, false),
    NN_PROXY_ITERATION_UPDATE_STATE("NNProxy状态更新", true, false),


    ZK_EXPANSION_UPDATE_CONF("zk扩容配置文件", true, false),

    ZK_EXPANSION_DEPLOY("zk扩容", true, true),

    ZK_EVICTION_UPDATE_CONF("zk缩容配置文件", true, false),

    ZK_EVICTION_DEPLOY("zk缩容", true, true),

    ZK_OTHER_NODE_REFRESH_CONFIG("其余节点重启并下发配置文件", true, true),

    ZK_NODE_REFRESH_CONFIG("重启并下发配置文件", true, true),

    ZK_RESTART("zk重启", true, true),

    ZK_CLUSTER_STATUS_CHECK("zk集群的状态检测", true, false),

    NN_PROXY_RESTART_PIPELINE_EVENT("NNProxy重启", true, true),


    /**
     * presto迭代发布
     */
    PRESTO_ITERATION_DEACTIVATE_CLUSTER("停止presto集群", true, false),
    PRESTO_ITERATION_DELETED_CLUSTER( "删除presto集群", false, false),
    PRESTO_ITERATION_DEPLOY_CLUSTER("部署presto集群", false, false),
    PRESTO_ITERATION_GLOBAL_CHECK_CLUSTER( "检查presto集群", false, false),
    PRESTO_ITERATION_ACTIVE_CLUSTER("启用presto集群", false, false),

    /**
     * trino experiment
     */
    TRINO_EXPERIMENT_CREATE_EXEC_EVENT("创建trino实验", false, false),
    TRINO_EXPERIMENT_QUERY_EXEC_EVENT("查询实验结果", false, false),
    ;

    private String desc;

    private boolean supportRetry;

    private boolean isDolphinType;

    EventTypeEnum(String desc, boolean supportRetry, boolean isDolphinType) {
        this.desc = desc;
        this.supportRetry = supportRetry;
        this.isDolphinType = isDolphinType;
    }

    public String getSummary() {
        return name() + ": " + getDesc();
    }
}
