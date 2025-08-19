package com.bilibili.cluster.scheduler.common.dto.dns.resp;

import com.bilibili.cluster.scheduler.common.dto.dns.DnsInfo;
import com.bilibili.cluster.scheduler.common.dto.dns.DnsPage;
import com.bilibili.cluster.scheduler.common.response.BaseResp;
import lombok.Data;

/**
 * @description:
 * @Date: 2025/4/22 11:51
 * @Author: nizhiqiang
 */

@Data
public class QueryDnsInfoListByIpResp extends BaseResp {

    DnsPage<DnsInfo> data;
}
