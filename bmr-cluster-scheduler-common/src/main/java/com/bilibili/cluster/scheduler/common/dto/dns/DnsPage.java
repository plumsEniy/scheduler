package com.bilibili.cluster.scheduler.common.dto.dns;

import cn.hutool.core.annotation.Alias;
import lombok.Data;

import java.util.List;

/**
 * @description: dns的分页
 * @Date: 2025/4/22 11:52
 * @Author: nizhiqiang
 */
@Data
public class DnsPage<T> {

    @Alias("total_size")
    int totalSize;

    @Alias("dns_records")
    List<T> dnsRecords;
}
