CREATE TABLE `scheduler_execution_flow`
(
    `id`                  bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT '自增id',
    `approve_uuid`        varchar(64)   NOT NULL DEFAULT '' COMMENT '封网审批的唯一id',
    `cluster_id`          bigint(20) unsigned NOT NULL DEFAULT 0 COMMENT '集群id',
    `component_id`        bigint(20) unsigned NOT NULL DEFAULT 0 COMMENT '组件id',
    `role_name`           varchar(30)   NOT NULL DEFAULT '' COMMENT '角色名称 yarn, clickhouse, hdfs... etc',
    `component_name`      varchar(100)  NOT NULL DEFAULT '' COMMENT '组件名称',
    `cluster_name`        varchar(60)   NOT NULL DEFAULT '' COMMENT ' 集群名称',
    `deploy_type`         varchar(36)   NOT NULL DEFAULT '0' COMMENT '变更类型：迭代、扩容、上下线',
    `release_scope_type`  varchar(36)   NOT NULL DEFAULT '0' COMMENT '发布类型：全量、灰度发布等',
    `deploy_package_type` varchar(36)   NOT NULL DEFAULT '0' COMMENT '包类型：安装包、配置包等',
    `restart`             tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否重启标记位',
    `effective_mode`      varchar(36)   NOT NULL DEFAULT '0' COMMENT '生效模式：重启｜立即生效',
    `approver`            varchar(256)  NOT NULL DEFAULT '' COMMENT '审批者',
    `package_id`          bigint(20) unsigned NOT NULL DEFAULT 0 COMMENT '安装包版本id',
    `config_id`           bigint(20) unsigned NOT NULL DEFAULT 0 COMMENT '配置包版本id',
    `job_execute_type`    varchar(64)   NOT NULL DEFAULT '0' COMMENT 'job执行方式',
    `auto_retry`          tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否自动重试',
    `max_retry`           int(11) NOT NULL DEFAULT 0 COMMENT '最大重试次数',
    `parallelism`         int(11) NOT NULL DEFAULT 0 COMMENT '并发度',
    `tolerance`           int(11) NOT NULL DEFAULT 0 COMMENT '容错度',
    `cur_fault`           int(11) NOT NULL DEFAULT 0 COMMENT '当前失败节点数',
    `start_time`          datetime      NOT NULL DEFAULT '1970-01-01 00:00:00' COMMENT '开始时间',
    `end_time`            datetime      NOT NULL DEFAULT '1970-01-01 00:00:00' COMMENT '结束时间',
    `flow_status`         varchar(32)   NOT NULL DEFAULT '' COMMENT '枚举类型字段：工作流执行状态',
    `ctime`               datetime      NOT NULL DEFAULT current_timestamp() COMMENT '创建时间',
    `mtime`               datetime      NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp () COMMENT '更新时间',
    `log_id`              bigint(20) unsigned NOT NULL DEFAULT 0 COMMENT '日志id',
    `deleted`             tinyint(1) NOT NULL DEFAULT 0 COMMENT '删除标识',
    `latest_active_time`  datetime      NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp () COMMENT '最新活跃时间',
    `flow_name`           varchar(255)  NOT NULL DEFAULT '' COMMENT '工作流名称',
    `flow_remark`         varchar(1000) NOT NULL DEFAULT '' COMMENT '发布单说明',
    `cur_batch_id`        int(11) NOT NULL DEFAULT 0 COMMENT '当前执行批次号',
    `max_batch_id`        int(11) NOT NULL DEFAULT 0 COMMENT '最大批次号',
    `operator`            varchar(64)   NOT NULL DEFAULT '' COMMENT '提交者',
    `props_id`            bigint(20) unsigned NOT NULL DEFAULT 0 COMMENT '参数id',
    `host_name`           varchar(64)   NOT NULL DEFAULT '' COMMENT '执行实例主机',
    `order_id`            varchar(50)   NOT NULL DEFAULT '' COMMENT '变更申请单id',
    `order_no`            varchar(50)   NOT NULL DEFAULT '' COMMENT '变更申请单No',
    `event_list`          text                   DEFAULT NULL COMMENT '阶段列表',
    PRIMARY KEY (`id`),
    KEY                   `ix_cluster_id` (`cluster_id`),
    KEY                   `ix_component_id` (`component_id`),
    KEY                   `ix_flow_status` (`flow_status`),
    KEY                   `ix_deploy_type` (`deploy_type`),
    KEY                   `ix_operator` (`operator`),
    KEY                   `ix_mtime` (`mtime`)
) ENGINE=InnoDB  DEFAULT CHARSET=utf8 COMMENT='工作流表';

CREATE TABLE `scheduler_execution_flow_props`
(
    `id`            bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT '自增id',
    `flow_id`       bigint(20) unsigned NOT NULL DEFAULT 0 COMMENT 'flow id',
    `props_content` text              DEFAULT NULL COMMENT '属性内容json',
    `ctime`         datetime NOT NULL DEFAULT current_timestamp() COMMENT '创建时间',
    `mtime`         datetime NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp () COMMENT '更新时间',
    `deleted`       tinyint(1) NOT NULL DEFAULT 0 COMMENT '删除标识',
    PRIMARY KEY (`id`),
    KEY             `ix_mtime` (`mtime`) USING BTREE,
    KEY             `ix_flow_id` (`flow_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='工作流参数表';

CREATE TABLE `scheduler_execution_node`
(
    `id`               bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT '自增id',
    `node_name`        varchar(128) NOT NULL DEFAULT '' COMMENT '节点名',
    `flow_id`          bigint(20) unsigned NOT NULL DEFAULT 0 COMMENT 'flow id',
    `batch_id`         int(10) unsigned NOT NULL DEFAULT 0 COMMENT '任务批次id',
    `extra_props_id`   bigint(20) unsigned NOT NULL DEFAULT 0 COMMENT '额外参数id',
    `operator`         varchar(64)  NOT NULL DEFAULT '' COMMENT '操作人',
    `node_status`      varchar(32)  NOT NULL DEFAULT '' COMMENT '节点状态',
    `deleted`          tinyint(1) NOT NULL DEFAULT 0 COMMENT '删除标识',
    `ctime`            datetime     NOT NULL DEFAULT current_timestamp() COMMENT '创建时间',
    `mtime`            datetime     NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp () COMMENT '更新时间',
    `start_time`       datetime     NOT NULL DEFAULT '1970-01-01 00:00:00' COMMENT '开始时间',
    `end_time`         datetime     NOT NULL DEFAULT '1970-01-01 00:00:00' COMMENT '结束时间',
    `operation_result` varchar(32)  NOT NULL DEFAULT '' COMMENT '任务的操作结果',
    `rack`             varchar(128) NOT NULL DEFAULT '' COMMENT '机架',
    `ip`               varchar(32)  NOT NULL DEFAULT '' COMMENT 'ip',
    PRIMARY KEY (`id`) USING BTREE,
    KEY                `ix_flow_id` (`flow_id`) USING BTREE,
    KEY                `ix_batch_id` (`batch_id`) USING BTREE,
    KEY                `ix_node_status` (`node_status`) USING BTREE,
    KEY                `ix_node_name` (`node_name`) USING BTREE,
    KEY                `ix_mtime` (`mtime`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=55 DEFAULT CHARSET=utf8 COMMENT='工作任务表';

CREATE TABLE `scheduler_execution_node_event` (
      `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT '自增id',
      `event_type` varchar(64) NOT NULL DEFAULT '' COMMENT '事件类型',
      `event_name` varchar(64) NOT NULL DEFAULT '' COMMENT '阶段名称',
      `execution_node_id` bigint(20) unsigned NOT NULL DEFAULT 0 COMMENT 'ExecutionNodeEntity 表ID',
      `flow_id` bigint(20) unsigned NOT NULL DEFAULT 0 COMMENT 'flow id',
      `batch_id` int(10) unsigned NOT NULL DEFAULT 0 COMMENT '任务批次id',
      `event_status` varchar(64) NOT NULL DEFAULT '' COMMENT '状态',
      `release_scope` varchar(64) NOT NULL DEFAULT '' COMMENT '范围状态',
      `host_name` varchar(255) NOT NULL DEFAULT '' COMMENT '执行实例主机名',
      `execute_order` int(11) NOT NULL DEFAULT 0 COMMENT '执行顺序',
      `log_id` bigint(20) unsigned NOT NULL DEFAULT 0 COMMENT '日志 id',
      `sched_instance_id` varchar(64) NOT NULL DEFAULT '' COMMENT 'dolphin的id',
      `start_time` datetime NOT NULL DEFAULT '1970-01-01 00:00:00' COMMENT '开始时间',
      `end_time` datetime NOT NULL DEFAULT '1970-01-01 00:00:00' COMMENT '结束时间',
      `ctime` datetime NOT NULL DEFAULT current_timestamp() COMMENT '创建时间',
      `mtime` datetime NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp() COMMENT '更新时间',
      `deleted` tinyint(1) NOT NULL DEFAULT 0 COMMENT '删除标识',
      PRIMARY KEY (`id`) USING BTREE,
      KEY `ix_mtime` (`mtime`) USING BTREE,
      KEY `ix_node_id` (`execution_node_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='工作任务事件表';

CREATE TABLE `scheduler_execution_node_props`
(
    `id`            bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '自增id',
    `node_id`       bigint unsigned NOT NULL DEFAULT '0' COMMENT 'node id',
    `props_content` text COMMENT '属性内容json',
    `ctime`         datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `mtime`         datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`       tinyint(1) NOT NULL DEFAULT '0' COMMENT '删除标识',
    PRIMARY KEY (`id`),
    KEY             `ix_mtime` (`mtime`) USING BTREE,
    KEY             `ix_node_id` (`node_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='工作任务参数表';

CREATE TABLE `scheduler_execution_log`
(
    `id`          bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '自增id',
    `log_type`    varchar(32) NOT NULL DEFAULT '' COMMENT '枚举类型字段：执行侧的日志类型',
    `execute_id`  bigint unsigned NOT NULL DEFAULT '0' COMMENT '外键id',
    `log_content` mediumtext COMMENT '日志内容',
    `deleted`     tinyint(1) NOT NULL DEFAULT '0' COMMENT '删除标识',
    `ctime`       datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `mtime`       datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY           `ix_mtime` (`mtime`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='日志表';

alter table scheduler_execution_node add column `exec_stage` varchar(32) NOT NULL DEFAULT '' COMMENT '执行所在阶段';

alter table scheduler_execution_node_event add column `project_code` varchar(64) NOT NULL DEFAULT '' COMMENT 'dolphin项目code';
alter table scheduler_execution_node_event add column `pipeline_Code` varchar(64) NOT NULL DEFAULT '' COMMENT 'dolphin项目code';
alter table scheduler_execution_node_event add column `task_code` varchar(64) NOT NULL DEFAULT '' COMMENT 'dolphin项目code';
alter table scheduler_execution_node_event add column `failure_strategy` varchar(64) NOT NULL DEFAULT '' COMMENT 'dolphin工作流失败策略';

alter table scheduler_execution_node_event add column `job_task_set_id` bigint(20) unsigned NOT NULL DEFAULT 0 COMMENT 'jobAgent结果集Id';
alter table scheduler_execution_node_event add column `job_task_id` bigint(20) unsigned NOT NULL DEFAULT 0 COMMENT 'jobAgent单任务Id';
alter table scheduler_execution_node_event add column `instance_id` bigint(20) unsigned NOT NULL DEFAULT 0 COMMENT '执行批次的实例Id';

alter table scheduler_execution_node add column `instance_id` bigint(20) unsigned NOT NULL DEFAULT 0 COMMENT '执行批次的实例Id';

alter table scheduler_execution_node_event add column `task_pos_type` varchar(64) NOT NULL DEFAULT '' COMMENT 'dolphin节点位置类型';

-- 20240903 flow 添加节点分组类型
alter table scheduler_execution_flow add column `group_type` varchar(64) NOT NULL DEFAULT 'RANDOM_GROUP' COMMENT '节点分组方式，默认:随机分组';
alter table scheduler_execution_flow_props modify column `props_content` mediumtext DEFAULT NULL COMMENT 'flow属性内容json';


alter table scheduler_execution_node_props modify column `props_content` mediumtext DEFAULT NULL COMMENT 'node属性内容json';

alter table scheduler_execution_node_v2 modify COLUMN node_name varchar(200) NOT NULL DEFAULT '' COMMENT '节点名';

alter table scheduler_execution_flow_v2 add COLUMN flow_rollback_type varchar(36) NOT NULL DEFAULT 'NONE' COMMENT '回滚类型';

alter table scheduler_execution_node_v2 add COLUMN node_type varchar(36) NOT NULL DEFAULT 'NORMAL' COMMENT '节点类型';
alter table scheduler_execution_node_v2 add COLUMN exec_type varchar(36) NOT NULL DEFAULT 'FORWARD' COMMENT '执行方式';

alter table scheduler_execution_node_v2 modify column `node_status` varchar(64) NOT NULL DEFAULT '' COMMENT '节点状态';

CREATE TABLE `scheduler_execution_flow_aop_event` (
      `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '自增id',
      `flow_id` bigint unsigned NOT NULL DEFAULT '0' COMMENT 'flow id',
      `count` int NOT NULL DEFAULT '0' COMMENT '触发次数',
      `event_type` varchar(36) NOT NULL DEFAULT '' COMMENT COMMENT '工作流事件类型',
      `ctime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
      `mtime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
      `deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '删除标识',
      `props` text COMMENT '额外参数',
      PRIMARY KEY (`id`) ,
      KEY           `ix_mtime` (`mtime`) USING BTREE,
      KEY           `ix_flow_id` (`flow_id`) USING BTREE

) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='工作流切片事件';

alter table `scheduler_execution_node_v2` add column `exec_host` varchar(50) NOT NULL DEFAULT '' COMMENT '执行所在主机';
