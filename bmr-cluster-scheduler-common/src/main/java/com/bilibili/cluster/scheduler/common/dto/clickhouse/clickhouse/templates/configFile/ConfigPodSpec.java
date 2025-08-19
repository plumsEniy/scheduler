package com.bilibili.cluster.scheduler.common.dto.clickhouse.clickhouse.templates.configFile;

import com.bilibili.cluster.scheduler.common.dto.clickhouse.clickhouse.templates.DnsConfig;
import lombok.Data;

/**
 * @description:
 * @Date: 2025/1/21 11:11
 * @Author: nizhiqiang
 */
@Data
public class ConfigPodSpec {

    DnsConfig dnsConfig;

    ConfigContainer container;

}
