启动: jvm添加如下参数:
-Ddeploy_env=uat -Dzone=sh001 -Ddiscovery_zone=sh001 -Dpleiades.config.enabled=false -Dpleiades.paladin.enabled=false -Dapp_id=infra.alter.dts-transfer-data-service

构建命令:
mvn clean package -Pdist -f pom.xml -Dmaven.test.skip=true -U


V2.0.55: release-note:
1、ALTER TABLE work_flow_sink_field DROP INDEX ix_sink_field_type;
2、ALTER TABLE work_flow_sink_field MODIFY COLUMN sink_field_type VARCHAR (15000) NOT NULL DEFAULT '' COMMENT '字段类型';
3、ALTER TABLE work_flow_task ADD sla VARCHAR(32) NOT NULL DEFAULT '' COMMENT 'SLA';

3、CREATE TABLE `work_flow_source_field`
(
`id`                  bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
`log_id`              varchar(64)   NOT NULL DEFAULT '' COMMENT '集成任务定义唯一标识',
`work_flow_source_id` bigint unsigned NOT NULL DEFAULT '0' COMMENT 'work_flow_source表主键',
`source_field_name`   varchar(128)  NOT NULL DEFAULT '' COMMENT '对应source字段名',
`source_field_type`   varchar(15000) NOT NULL DEFAULT '' COMMENT '字段类型',
`deleted`             tinyint(1) NOT NULL DEFAULT '0' COMMENT '删除标记',
`ctime`               datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
`mtime`               datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
`creator`             varchar(64)   NOT NULL DEFAULT '' COMMENT '创建人',
`updater`             varchar(64)   NOT NULL DEFAULT '' COMMENT '修改人',
PRIMARY KEY (`id`),
KEY                   `ix_mtime` (`mtime`),
KEY                   `ix_log_id` (`log_id`),
KEY                   `work_flow_source_id` (`work_flow_source_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='集成任务定义数据源端字段详情';

4、修正历史work_flow_sink_field 表字段数据

5、发生Resolver xxx await timeout, maybe service is not register 异常时通过以下方式处理：
添加本机host解析 127.0.0.1      yourHostname   (Terminal输入hostname的返回结果，比如 xxx-MacBook-Pro.local，那么host文件中配置 127.0.0.1   xxx-MacBook-Pro.local)。注意IPV6也需要添加， ::1  xxx-MacBook-Pro.local


