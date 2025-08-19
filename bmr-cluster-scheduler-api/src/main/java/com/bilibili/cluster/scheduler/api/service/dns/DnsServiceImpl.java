package com.bilibili.cluster.scheduler.api.service.dns;

import cn.hutool.core.net.url.UrlBuilder;
import cn.hutool.http.HttpRequest;
import cn.hutool.json.JSONUtil;
import com.bilibili.cluster.scheduler.common.dto.dns.DnsInfo;
import com.bilibili.cluster.scheduler.common.dto.dns.resp.QueryDnsInfoListByIpResp;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @description:
 * @Date: 2025/4/22 11:31
 * @Author: nizhiqiang
 */

@Slf4j
@Service
public class DnsServiceImpl implements DnsService {
    @Value("${dns.base-url:http://dns.bilibili.co}")
    private String BASE_URL;


    @Override
    public List<DnsInfo> queryDnsInfoListByIp(String ip, int pageNum, int pageSize) {
        String url = UrlBuilder.ofHttp(BASE_URL)
                .addPath("/api/v1/dns_mng/open/dns_record")
                .addQuery("page_num", String.valueOf(pageNum))
                .addQuery("page_size", String.valueOf(pageSize))
                .addQuery("record_value", ip)
                .build();

        String respStr = HttpRequest.get(url)
                .header("Host", "dns.bilibili.co")
                .execute()
                .body();

        log.info("query dns info list by ip resp is :{}", respStr);
        QueryDnsInfoListByIpResp resp = JSONUtil.toBean(respStr, QueryDnsInfoListByIpResp.class);
        return resp.getData().getDnsRecords();
    }
}
