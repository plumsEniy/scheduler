package com.bilibili.cluster.scheduler.api.service.hadoop;

import cn.hutool.core.lang.TypeReference;
import cn.hutool.core.net.url.UrlBuilder;
import cn.hutool.http.HttpRequest;
import cn.hutool.json.JSONUtil;
import com.bilibili.cluster.scheduler.common.dto.hadoop.FastDecommissionTaskDTO;
import com.bilibili.cluster.scheduler.common.dto.hadoop.req.CreateFastDecommissionReq;
import com.bilibili.cluster.scheduler.common.dto.hadoop.req.StartFastDecommissionReq;
import com.bilibili.cluster.scheduler.common.enums.dataNode.DataNodeVersionEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;

/**
 * @description:
 * @Date: 2024/5/13 10:46
 * @Author: nizhiqiang
 */
@Slf4j
@Service
public class FastDecomissionServiceImpl implements FastDecomissionService {

    private final String BASE_URL = "http://hadoop-tools.bilibili.co";

    @Override
    public Long createFastDecommission(List<String> dataNodeList) {
        String url = UrlBuilder.ofHttp(BASE_URL)
                .addPath("/hadoop-tools/fastdecommission/dnconfig/create")
                .build();
        CreateFastDecommissionReq req = new CreateFastDecommissionReq(dataNodeList);
        log.info("create fast decomission req is {}", JSONUtil.toJsonStr(req));
        String respStr = HttpRequest.post(url)
                .body(JSONUtil.toJsonStr(req))
                .execute().body();
        log.info("create fast decomission resp is {}", respStr);
        return Long.valueOf(respStr);
    }

    @Override
    public void startFastDecommission(Long dnId, DataNodeVersionEnum dataNodeVersion) {
        String url = UrlBuilder.ofHttp(BASE_URL)
                .addPath("/hadoop-tools/fastdecommission/create")
                .build();
        StartFastDecommissionReq req = new StartFastDecommissionReq();
        req.setDnId(dnId);
//        目前只有jscs这个一个集群，写死dc
        req.setDc("jscs");
        switch (dataNodeVersion) {
            case VERSION2:
                req.setNodes(Collections.emptyList());
                req.setHadoopConfDirs(Collections.emptyList());
                break;
            case VERSION3:
                req.setClusterVersion("3.3");
                break;
            default:
                throw new IllegalArgumentException("未处理的datanode版本" + dataNodeVersion);
        }
        log.info("start fast decomission req is {}", JSONUtil.toJsonStr(req));

        String resp = HttpRequest.post(url)
                .body(JSONUtil.toJsonStr(req))
                .execute().body();

        log.info("start fast decomission resp is {}", JSONUtil.toJsonStr(resp));

    }

    @Override
    public List<FastDecommissionTaskDTO> queryFastDecommission(Long dnId) {
        String url = UrlBuilder.ofHttp(BASE_URL)
                .addPath("/hadoop-tools/fastdecommission/queryv2")
                .addQuery("dnConfigId", String.valueOf(dnId))
                .build();

        String respStr = HttpRequest.get(url)
                .execute().body();

        log.info("query fast decomission resp is {}", respStr);
        if (StringUtils.isEmpty(respStr)) {
            return null;
        }
        List<FastDecommissionTaskDTO> resp = JSONUtil.toBean(respStr, new TypeReference<List<FastDecommissionTaskDTO>>() {
        }, false);

        return resp;
    }
}
