package com.bilibili.cluster.scheduler.api.service.hadoop;

import cn.hutool.core.net.url.UrlBuilder;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONUtil;
import com.bilibili.cluster.scheduler.common.Constants;
import com.bilibili.cluster.scheduler.common.dto.hadoop.NameNodeFsIndex;
import com.bilibili.cluster.scheduler.common.dto.hadoop.resp.QueryNameNodeFsIndexResp;
import com.bilibili.cluster.scheduler.common.utils.ObjectMapperUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @description: namenode服务
 * @Date: 2024/5/13 15:10
 * @Author: nizhiqiang
 */

@Service
@Slf4j
public class NameNodeServiceImpl implements NameNodeService, InitializingBean {

    private ObjectMapper objectMapper;

    private final String BASE_URL = "http://%s:%s";

    @Override
    public List<NameNodeFsIndex> queryNameNodeFsIndex(String nameNodeHostName) {
        String baseUrl = getBaseUrl(nameNodeHostName, 50070);
        String url = UrlBuilder.ofHttp(baseUrl)
                .addPath("/jmx")
                .addQuery("qry", "Hadoop:service=NameNode,name=FSNamesystem")
                .build();

        log.info("query name node fs index, url is {}", url);

        String respStr = HttpRequest.get(url)
                .execute().body();
        log.info("query name node fs index, resp is {}", respStr);

        QueryNameNodeFsIndexResp resp = JSONUtil.toBean(respStr, QueryNameNodeFsIndexResp.class);
        return resp.getBeans();
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        objectMapper = ObjectMapperUtil.getCommonObjectMapper();
    }

    public String getBaseUrl(String hostName, Integer httpPort) {
        if (!hostName.endsWith(Constants.HOST_SUFFIX)) {
            hostName = hostName.concat(Constants.HOST_SUFFIX);
        }
        return String.format(BASE_URL, hostName, httpPort);
    }
}
