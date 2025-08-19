
package com.bilibili.cluster.scheduler.common;


import java.time.Duration;
import java.util.regex.Pattern;

public final class Constants {

    public static final String MSG_TYPE_TEXT = "text";
    public static final String SCHEDULER = "SCHEDULER";
    public static final String PAUSE = "PAUSE";
    public static final String FAIL = "FAIL";
    public static final String NAN = "NAN";
    public static final String BATCH_ID = "BATCH_ID";
    public static final String EXEC_NODE_LIST = "exeNodeList";
    public static final String FLOW_PROPS_PARAMS = "flowProps";
    public static final String FLOW_EXT_PARAMS = "flowExtParams";
    public static final String FLOW_MAX_STAGE_KEY = "flowMaxStageKey";
    public static final String FLOW_MIN_STAGE_KEY = "flowMinStageKey";
    public static final String MAX_NODE_ID_BY_STAGE_KEY = "maxNodeIdByStageKey";

    public static final String MONITOR_PIPELINE_FACTORY_IDENTIFY = "Monitor";

    public static final String SPARK_DEPLOY_PIPELINE_FACTORY_IDENTIFY = "Spark-Deploy";
    public static final String SPARK_VERSION_LOCK_PIPELINE_FACTORY_IDENTIFY = "Spark-Version-Lock";
    public static final String SPARK_EXPERIMENT_PIPELINE_FACTORY_IDENTIFY = "Spark-Experiment";
    public static final String SPARK_CLIENT_PACKAGE_DEPLOY_FACTORY_IDENTIFY = "Spark-Client-Package-Deploy";

    public static final String HBO_JOB_PARAMS_UPDATE_DEPLOY_FACTORY_IDENEITFY = "Hbo-Job-Params-Update-Deploy-Factory";

    public static final String HBO_JOB_PARAMS_DELETE_DEPLOY_FACTORY_IDENEITFY = "Hbo-Job-Params-Delete-Deploy-Factory";

    public static final String CK_CONTAINER_DEPLOY_FACTORY_IDENTIFY = "ck-container-deploy-factory";


    public static final String ZK_DEPLOY_PIPELINE_FACTORY_IDENTIFY = "zk-deploy-factory";


    public static final String PRESTO_TIDE_DEPLOY_FACTORY_IDENTIFY = "Presto-Tide-Deploy";
    public static final String PRESTO_TO_PRESTO_TIDE_DEPLOY_FACTORY_IDENTIFY = "Presto-To-Presto-Tide-Deploy";

    public static final String CK_TIDE_DEPLOY_FACTORY_IDENTIFY = "ck-Tide-Deploy";
    public static final String SPARK_PERIPHERY_COMPONENT_DEPLOY_FACTORY_IDENTIFY = "Spark-Periphery-Component-Deploy";
    public static final String NN_PROXY_DEPLOY_FACTORY_IDENTIFY = "NN-Proxy-Deploy";
    public static final String NN_PROXY_RESTART_FACTORY_IDENTIFY = "NN-Proxy-Restart";

    public static final String PRESTO_FAST_SCALER_IDENTIFY  = "PRESTO-FAST-SCALER";

    public static final String TRINO_EXPERIMENT_PIPELINE_FACTORY_IDENTIFY  = "TRINO-EXPERIMENT-IDENTIFY";

    public static final String YARN_TIDE_IDENTIFY = "YARN-TIDE";


    public static final double PERCENT_FACTOR = 0.01;
    public static final String DEFAULT_EXEC_STAGE = "1";
    public static final String PERCENT_SIGNAL = "%";

    public static final String BMR_UNIFIED_OA_PROCESS_NAME = "BMR-变更审批";
    public static final String EXPERIMENT_PLATFORM_EMPTY_VALUE = "empty_default";
    public static final String DOLPHIN_SCHEDULER_TOKEN_KEY = "dolphin.scheduler.token";
    public static final String HTTP_PROTOCOL = "http://";
    public static final String PRESTO_TIDE_NODE_GROUP_NAME = "presto潮汐混部";

    public static final String CK_TIDE_NODE_GROUP_NAME = "clickhouse潮汐混部";
    public static final String TRINO_POOL_NAME = "trino";


    public static final String PRESTO_TAINT_KEY = "presto-tide";

    public static final String PRESTO_TAINT_VALUE = "presto-to-yarn-node";

    public static final String CK_TAINT_KEY = "clickhouse-tide";

    public static final String CK_TAINT_VALUE = "clickhouse-to-yarn-node";


    public static final String CK_ON_ICEBERG_POOL_NAME = "clickhouse_on_iceberg_pool";
    public static final String TIDE_ON_YARN_NODE_LABEL = "elasticLabel";
    public static final String FLOW_ENV_KEY = "FLOW_ENV";
    public static final String YARN_RM_COMPONENT_ID_KEY = "YARN_RM_COMPONENT_ID";
    public static final String CLICK_HOUSE_ROLE = "clickhouse";

    public static final String CLICK_HOUSE_COMPONENT = "ClickHouse";
    public static final String NN_PROXY = "NNProxy";

    private Constants() {
        throw new UnsupportedOperationException("Construct Constants");
    }

    public static final String EMPTY_STRING = "";

    public static final Duration SERVER_CLOSE_WAIT_TIME = Duration.ofSeconds(3);

    public static final String LOG_TRACE_ID = "traceId";

    public static final String SESSION_KEY = "_AJSESSIONID";

    public static final String USER = "user";

    /**
     * 根据sessionId 获取登陆的用户名
     */
    public static final String REQUEST_USER = "login_user";

    public static final String DEFAULT_LEAF_TAG = "noLeafTag";

    public static final int MIN_PAGE_SIZE = 1;
    public static final int DEFAULT_PAGE_NUM = 1;
    public static final int PAGE_MAX = 9999999;


    public static final long DEFAULT_PADDING_LONG_VALUE = 0l;


    /**
     * saber服务
     */
    public static final String SABER = "saber";

    public static final String MULTI_SLASH = "/{2,}";

    public static final String SLASH = "/";

    public static final String BAR = "-";

    public static final String COMMA = ",";
    public static final String POINT = ".";

    public static final String TAG_NAME = "tag";

    public static final String DELETED = "deleted";


    public static final String CTIME = "ctime";

    public static final String LIMIT_ONE = "limit 1";

    public static final String BILIBILI_HOST_SUFFIX = ".host.bilibili.co";

    public static final String SABER_SESSION_JOB = "session-job";
    public static final String SABER_PER_JOB = "per-job";

    public static final String HOSTS_DEFAULT_LABEL = "<DEFAULT_PARTITION>";

    public static final String CASTER_TOKEN_KEY = "X-Authorization-Token";

    public static final String AUTH_PLATFORM_API_TOKEN = "b21lZ2EtZmxpbmstbWFuYWdlci1jbWRiCg==";
    public static final String AUTH_PLATFORM_ID = "omega-flinkmanager-cmdb";

    public static final String K8S_LABEL_KEY = "pool";

    public static final String TRUE_VALUE = "True";

    public static final String UNKNOWN = "unknown";

    public static final String TENANT_NAME_KEY = "Alter-tenant";
    public static final String TENANT_TOKEN_KEY = "Alter-token";
    public static final String QUERY_DUTY_OPERATOR_TOKEN = "JmtMs0X1JrO1Dgu4UylKLihKD2";

    public static final String COOKIES_KEY = "Cookie";

    public static final String COOKIE = "Cookie";
    public static final String TOKEN = "token";

    public static final String CONTENT_TYPE = "Content-Type";

    public static final String APPLICATION_JSON = "application/json";
    public static final String UTF_8 = "UTF-8";

    public static final String SABER_ADMIN_NAME = "liqiang03";


    public static final long ONE_SECOND = 1000;
    public static final long TEN_MINUTES = 10 * 60 * 1000;
    public static final long ONE_MINUTES = 60 * 1000;

    public static String ZK_INSTANCE_UID = "INSTANCE_UID";

    public static String ZK_OP_STATUE = "OP_STATE";

    public static String ZK_OP_STRATEGY = "OP_STRATEGY";

    public static String ZK_OP_FAILURE_UID = "OP_FAILURE_UID";

    public static String ZK_OP_RETRY_FAILURE_UID = "ZK_OP_RETRY_FAILURE_UID";

    public static final String LOCAL_ADDRESS = "127.0.0.1";

    public static final String UNDER_LINE = "_";
    public static final String LINE = "-";

    public static final String FMT_MONTH = "yyyy-MM";
    public static final String FMT_DAY = "yyyy-MM-dd";
    public static final String FMT_DATE_TIME = "yyyy-MM-dd HH:mm:ss";
    public static final String FMT_MILLS = "yyyy-MM-dd HH:mm:ss.SSS";
    public static final String FMT_HOUR = "yyyy-MM-dd HH";
    public static final String FMT_MINS = "yyyy-MM-dd HH:mm";
    public static final String FMT_DATE_TIME_UNIT = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
    public static final String FMT_DATE_TIME_UNIT_SECOND = "yyyy-MM-dd'T'HH:mm:ss'Z'";
    public static final String FMT_DATE_TIME_SECOND = "yyyyMMddHHmmss";

    public static final String UAT_ENV = "uat";
    public static final String ENV_KEY = "running_env";
    public static final String PROD_ENV = "prod";
    public static final String PRE_ENV = "pre";

    public static final String NEW_LINE = "\n";
    // 1 MB size
    public static int MSG_MAX_SIZE = 1 << 20;
    public static final String EXEC_FLOW_ID = "flow_id";
    public static final String REGISTRY_DTS_MASTERS = "/nodes/master";

    public static final String REGISTRY_DTS_NODE = "/nodes";

    public static final long SLEEP_TIME_MILLIS = 10_000L;

    public static final String SINGLE_SLASH = "/";

    public static final String DIVISION_STRING = "/";

    public static final String COLON = ":";

    public static final String REGISTRY_DTS_LOCK_MASTERS = "/lock/masters";

    public static final String REGISTRY_DTS_LOCK_FAILOVER_MASTERS = "/lock/failover/masters";

    public static final String NULL = "NULL";

    public static final String SUBMIT_FLOW = "SUBMIT_FLOW";

    public static final String WORK_FLOW_PROCESS_INSTANCE_ID_MDC_KEY = "workFlowProcessInstanceId";

//    public static final String UPDATE_WORK_FLOW_PROCESS_KEY = "UPDATE_WORK_FLOW_PROCESS_KEY";
//    public static final String UPDATE_WORK_FLOW_PROCESS_EVENT_KEY = "UPDATE_WORK_FLOW_PROCESS_EVENT_KEY";

    public static final String EXECUTE_FLOW_KEY = "EXECUTE_FLOW";

    public static final String CUSTOM_CLUSTER_CONFIG = "--customClusterConfig";

    public static final String FLINK_TAG = "--flinkTag";

    public static final String SKIP_EVICTION_JOB_CONF = "temp.useless.execution.checkpointing.approximate-local-recovery.enabled=true";

    /**
     * manager指标参数
     */
    public static final String TASK_MANAGER_METRIC = "taskmanager_Status_JVM_CPU_Load1";
    public static final String JOB_MANAGER_METRIC = "jobmanager_job_numTaskManagers";

    /**
     * oa审批参数
     */
    public static final String FLINK_MANAGER_PROCESS_NAME = "bmr-flinkmanager发布变更";
    public static final String SPARK_MANAGER_PROCESS_NAME = "spark-manager";

    public static final String K8S_1 = "K8S-1";
    public static final String JSCS_YARN_NM = "JSCS-Yarn-NM";

    public static String BMR_SYSTEM_INVOKER_KEY = "BMR-xxxxxxxxx-TOKEN";
    public static String BMR_SYSTEM_TOKEN = "xxxxxxxxxxxxxxx";

    public static String EQUAL = "=";

    public static String AND = "&";

    public static String QUESTION_MARK = "?";

    public static String SEMI_COLON = ";";

    /**
     * 微信通知
     */

    public static final int NOTIFY_TEAM = 2490;

    public static final int NOTIFY_COMPOMENT_ID = 114;

    public static final String NOTIFY_COMPOMENT = "FLINK";

    public static final int MAX_NOTIFY_TASK_COUNT = 50;

    public static final String ONLINE = "ONLINE";
    public static final Long ONLINE_TEAM_ID = 23L;

    public static final String WX_PUSH_TOKEN = "jIjnIXdGleP2jH2fictPDLvATtACBN5U";

    public static final String ULTRON = "ultron";

    public static final String HDFS = "hdfs";
    public static final String PRESTO = "presto";
    public static final String PRESTO_COMPONENT = "presto";

    /**
     * 主机名后缀
     */
    public static final String HOST_SUFFIX = ".host.bilibili.co";

    /**
     * datanode eviction锁
     */
    public static final String DN_FASTDECOMISSION_LOCK = "DN_FASTDECOMISSION_LOCK";

    public static final String FASTDECMISSION_FILE = "fastdecommissions";
    public static final String MAINTENANCE_FILE = "maintenance";
    public static final String BALANCER_EXCLUDES_FILE = "balancer-excludes";
    public static final String NAME_NODE_EXCLUDES_FILE = "excludes";
    public static final String NAME_NODE_INCLUDE_FILE = "includes";
    public static final String COMPONENT_ROLE = "COMPONENT_ROLE";
    public static final String COMPONENT_CLUSTER = "COMPONENT_CLUSTER";
    public static final String FLOW_ID = "flowId";
    public static final String _JOB_EXCUTE_TYPE = "_JOB_EXCUTE_TYPE";
    public static final String RELASE_SCOPE = "RELASE_SCOPE";
    public static final String COMPONENT_NAME = "COMPONENT_NAME";
    public static final String DOWNLOAD_DIR_VALUE = "/data/src/allpack/";
    public static final String DOWNLOAD_DIR = "DOWNLOAD_DIR";
    public static final String DEPLOYMENT_ORDER_CREATOR = "DEPLOYMENT_ORDER_CREATOR";
    public static final String NODE_WARNINGS_NUMBER = "NODE_WARNINGS_NUMBER";
    public static final String SYSTEM_JOBAGENT_EXEC_HOSTS = "_SYSTEM_JOBAGENT_EXEC_HOSTS";
    public static final String SERVICE_RESTART = "SERVICE_RESTART";
    public static final String EFFECTIVE_MODE = "EFFECTIVE_MODE";
    public static final String CI_PACK_ID = "CI_PACK_ID";
    public static final String CI_PACK_MD5 = "CI_PACK_MD5";
    public static final String CI_PACK_NAME = "CI_PACK_NAME";
    public static final String CI_PACK_TAG_NAME = "CI_PACK_TAG_NAME";
    public static final String CI_PACK_VERSION = "CI_PACK_VERSION";
    public static final String CI_PACK_URL = "CI_PACK_URL";
    public static final String HOST_ENV_MAP_KEY = "HOST_ENV_MAP";

    public static final String CONFIG_PACK_ID = "CI_TEMPLATE_ID";
    public static final String CONFIG_PACK_MD5 = "CI_TEMPLATE_PACK_MD5";
    public static final String CONFIG_PACK_NAME = "CI_TEMPLATE_PACK_NAME";
    public static final String CONFIG_PACK_URL = "CI_TEMPLATE_PACK_URL";
    public static final String CONFIG_PACK_VERSION = "CI_TEMPLATE_VERSION";
    public static final String NODE_GROUP_NAME = "NODE_GROUP_NAME";

    // 潮汐能力
    public static final String NODEMANAGER_IS_EXECUTE = "NODEMANAGER_IS_EXECUTE";
    public static final String SPARK_IS_EXECUTE = "SPARK_IS_EXECUTE";
    public static final String AMIYA_IS_EXECUTE = "AMIYA_IS_EXECUTE";
    public static final String SPARK_COMPONENT_NAME = "SPARK_COMPONENT_NAME";
    public static final String AMIYA_COMPONENT_NAME = "AMIYA_COMPONENT_NAME";
    public static final String SPARK_DOWNLOAD_DIR = "SPARK_DOWNLOAD_DIR";
    public static final String AMIYA_DOWNLOAD_DIR = "AMIYA_DOWNLOAD_DIR";

    public static final String SPARK = "spark";

    public static final String FALSE = "false";
    public static final String TRUE = "true";

    public static final String SPARK_CONFIG_NODE_GROUP = "SPARK_CONFIG_NODE_GROUP";
    public static final String AMIYA_CONFIG_NODE_GROUP = "AMIYA_CONFIG_NODE_GROUP";

    /**
     * 潮汐上线额外脚本参数
     */
    public static final String SPARK_CI_PACK_ID = "SPARK_CI_PACK_ID";
    public static final String SPARK_CI_PACK_MD5 = "SPARK_CI_PACK_MD5";
    public static final String SPARK_CI_PACK_NAME = "SPARK_CI_PACK_NAME";
    public static final String SPARK_CI_PACK_TAG_NAME = "SPARK_CI_PACK_TAG_NAME";
    public static final String SPARK_CI_PACK_VERSION = "SPARK_CI_PACK_VERSION";
    public static final String SPARK_CI_PACK_URL = "SPARK_CI_PACK_URL";

    public static final String SPARK_CONFIG_PACK_ID = "SPARK_CI_TEMPLATE_ID";
    public static final String SPARK_CONFIG_PACK_MD5 = "SPARK_CI_TEMPLATE_PACK_MD5";
    public static final String SPARK_CONFIG_PACK_NAME = "SPARK_CI_TEMPLATE_PACK_NAME";
    public static final String SPARK_CONFIG_PACK_URL = "SPARK_CI_TEMPLATE_PACK_URL";
    public static final String SPARK_CONFIG_PACK_VERSION = "SPARK_CI_TEMPLATE_VERSION";

    public static final String AMIYA_CI_PACK_ID = "AMIYA_CI_PACK_ID";
    public static final String AMIYA_CI_PACK_MD5 = "AMIYA_CI_PACK_MD5";
    public static final String AMIYA_CI_PACK_NAME = "AMIYA_CI_PACK_NAME";
    public static final String AMIYA_CI_PACK_TAG_NAME = "AMIYA_CI_PACK_TAG_NAME";
    public static final String AMIYA_CI_PACK_VERSION = "AMIYA_CI_PACK_VERSION";
    public static final String AMIYA_CI_PACK_URL = "AMIYA_CI_PACK_URL";

    public static final String AMIYA_CONFIG_PACK_ID = "AMIYA_CI_TEMPLATE_ID";
    public static final String AMIYA_CONFIG_PACK_MD5 = "AMIYA_CI_TEMPLATE_PACK_MD5";
    public static final String AMIYA_CONFIG_PACK_NAME = "AMIYA_CI_TEMPLATE_PACK_NAME";
    public static final String AMIYA_CONFIG_PACK_URL = "AMIYA_CI_TEMPLATE_PACK_URL";
    public static final String AMIYA_CONFIG_PACK_VERSION = "AMIYA_CI_TEMPLATE_VERSION";

    public static final String SUB_SYSTEM_HOST_LIST = "_SUB_SYSTEM_HOST_LIST";

    public static final String HA_ACTIVE = "ACTIVE";

    public static final String SUCCESS = "SUCCESS";

    public static final String NUM_SSD = "numSsd";
    public static final String NUM_NVME = "numNvme";
    public static final String NUM_SATA = "numSata";

    public static final String CONFIG_NODE_GROUP = "CONFIG_NODE_GROUP";

    public static final String YARN_INCLUDE = "yarn-include";
    public static final String YARN_EXCLUDE = "yarn_exclude";
    public static final String YARN_INCLUDE_DOWNLOAD_URL = "YARN_INCLUDE_DOWNLOAD_URL";
    public static final String YARN_INCLUDE_FILE_MD5 = "YARN_INCLUDE_FILE_MD5";
    public static final String YARN_EXCLUDE_DOWNLOAD_URL = "YARN_EXCLUDE_DOWNLOAD_URL";
    public static final String YARN_EXCLUDE_FILE_MD5 = "YARN_EXCLUDE_FILE_MD5";

    public static final String APPLICATION_STATE_RUNNING = "RUNNING";

    public static final String SPARK_BLACK_LIST = "blacklist";

    public static final long DEFAULT_ERROR_SKIP_STEP_SIZE = 10L;

    public static final String SPLIT = "---------------------";

    public static final Integer BLOCKS_THRESHOLD = 10;

    public static final String FILE_NAME = "FILE_NAME";
    public static final String FILE_DOWNLOAD_URL = "FILE_DOWNLOAD_URL";
    public static final String FILE_MD5 = "FILE_MD5";

    public static final String HADOOP_DN_VERSION = "HADOOP_DN_VERSION";

    public static final String FOMR_URLENCODED = "application/x-www-form-urlencoded";


    public static final String CATALOG_FILE = "catalogSpec.xml";
    public static final String CLUSTER_FILE = "cluster.xml";

    public static final String COORDINATOR_FILE = "coordinator.xml";
    public static final String ADDITIONAL_PROPS = "_additionalProps";

    public static final String COORDINATOR_ADDITIONALPROPS_FILE_PRE_FIX = "coordinator_additionalProps";


    public static final String RESOURCE_FILE = "resource.xml";
    public static final String RESOURCE_ADDITIONALPROPS_FILE_PRE_FIX = "resource_additionalProps";

    public static final String WORKER_FILE = "worker.xml";
    public static final String WORKER_ADDITIONALPROPS_FILE_PRE_FIX = "worker_additionalProps";

    public static final String WORKER = "worker";

    public static final String RESOURCE = "resource";

    public static final String COORDINATOR = "coordinator";

    public static final String PRESTO_COUNT = "count";

    public static final String CK_STABLE_TEMPLATE = "clickhouse-stable";

    public static final String CK_ADMIN_SHARDS_FILE = "cluster_admin_shards.yaml";

    public static final String CK_ALTINTY_COM = "clickhouse.altinity.com/chi";

    public static final Long CK_K8S_CLUSTER_ID = 93L;

    public static final String POD_STATUS_RUNNING = "Running";
    public static final String POD_STATUS_SUCCESS = "SUCCESS";
    public static final String POD_STATUS_FAILED = "Running";

    public static final String PRESTO_ADDITIONAL_PROPS_FILE = "presto_additionalProps.xml";

    public static final String DEFAULT_GROUP_NAME = "节点组default默认配置";

    public static final String START_CATALOG = "catalog_";
    public static final String XML_SUFFIX = ".xml";
    public static final String PROPERTIES_SUFFIX = ".properties";


    public static final String UAT_PRESTO_BASE_URL = "http://pre-presto-gateway.bilibili.co";
    public static final String PRE_PRESTO_BASE_URL = "http://pre-presto-gateway.bilibili.co";
    public static final String PROD_PRESTO_BASE_URL = "http://presto-gateway.bilibili.co";

    public static final String MAIN = "main";
    public static final String SLEEP_SH = "sleep.sh";


    public static final String DATA_NODE_COMPONENT_NAME = "DataNode";

    public static final String PRESTO_COMPONENT_NAME = "Presto";


    public static final String PRESTO_CASTER_CLUSTER_NAME = "jscs-trino-k8s";

    public static final String END_XML = ".xml";
    public static final String PRODUCT = "product";
    public static final String RUNNING_APPLICATION_STATE = "RUNNING";
    public static final String UNHEALTHY_APPLICATION_STATE = "UNHEALTHY";
    public static final String LOST_APPLICATION_STATE = "LOST";

    // for spark client install
    public static final String SPARK_CLIENT_PACK_LIST_KEY = "SPARK_CLIENT_PACK_LIST";

    public static final String SPARK_CLIENT_PACK_TYPE_KEY = "SPARK_CLIENT_TYPE";
    public static final String SPARK_CLIENT_PACK_DOWNLOAD_URL_KEY = "SPARK_CLIENT_PACK_DOWNLOAD_URL";
    public static final String SPARK_CLIENT_PACK_MD5_KEY = "SPARK_CLIENT_PACK_MD5";
    public static final String SPARK_CLIENT_PACK_NAME_KEY = "SPARK_CLIENT_PACK_NAME";


    public static final String POD_TEMPLATES_FILE = "podTemplates.yaml";

    public static final String SERVICE_TEMPLATES_FILE = "serviceTemplates.yaml";

    public static final String VOLUME_TEMPLATES_FILE = "volumeClaimTemplates.yaml";

    public static final String PASS_CONFIG_FILE = "paasConfig.xml";

    public static final String CONFIG_SETTINGS_FILE = "config_settings.xml";

    public static final String PROFILES_FILE = "profiles.xml";

    public static final String ZOOKEEPER_FILE = "zookeeper.yaml";

    public static final String USERS_FILE = "users.xml";

    public static final String STORAGE_FILE = "storage.xml";

    public static final String CLICKHOUSE_USERS_IP = "default/networks/ip";

    public static final String REPLICA_PROPS_FILE = "cluster_replica_props.xml";

    public static final String REPLICA_SHARDS_FILE = "cluster_replica_shards.yaml";

    public static final String ADMIN_PROPS_FILE = "cluster_admin_props.xml";

    public static final String CK_K8S_CLUSTER = "jscs-clickhouse-k8s";

    public static final String ADMIN_SHARDS_FILE = "cluster_admin_shards.yaml";

    public static final String REPLICA = "replica";

    public static final String ADMIN = "admin";

    public static final String SHARD = "shard";

    public static final String IS_APPROVAL_NO = "no";

//    public static final String CK_K8S_TEST_HOST_IP = "10.155.15.25";
//    public static final String CK_K8S_TEST_HOST = "jscs-datacenter-olap-ck-k8s-node-29";

    public static final String CK_K8S_TEST_HOST_IP_1 = "10.155.15.24";
    public static final String CK_K8S_TEST_HOST_1 = "jscs-datacenter-olap-ck-k8s-node-31";


    public static final String INCIDENT_PLATFORM = "大数据集群管理平台 (BMR)";

    public static final String ZOO_CONFIG = "zoo.cfg";

    public static final String ZK_COMPONENT = "zookeeper";

    public static final String ZK_SERVER_CONFIG_FILE_NAME = "zoo.cfg";

    public static final String ZK_CONF_URL = "ZK_CONF_URL";

    public static final String ZK_CONF_MD5 = "ZK_CONF_MD5";

    public static final String ZK_LEADER = "leader";

    public static final String RUNNING_STATUS = "RUNNING";


    public static final Pattern DNS_PATTERN = Pattern.compile("^(?!-)[a-zA-Z0-9-]+(?<!-)(\\.(?!-)[a-zA-Z0-9-]+(?<!-))*\\.(com|org|net|edu|int|gov|mil|co)$");

}
