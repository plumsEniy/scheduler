package com.bilibili.cluster.scheduler.api.service.dns;

import com.bilibili.cluster.scheduler.common.dto.dns.DnsInfo;

import java.util.List;

/**
 * @description:
 * @Date: 2025/4/22 11:31
 * @Author: nizhiqiang
 */
public interface DnsService {

    List<DnsInfo> queryDnsInfoListByIp(String ip, int pageNum, int pageSize);
}
