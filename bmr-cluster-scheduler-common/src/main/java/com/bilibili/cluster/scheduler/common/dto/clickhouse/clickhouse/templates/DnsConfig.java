package com.bilibili.cluster.scheduler.common.dto.clickhouse.clickhouse.templates;

import lombok.Data;

import java.util.List;

@Data
public class DnsConfig {
    private List<String> searches;
    private List<String> nameservers;
}