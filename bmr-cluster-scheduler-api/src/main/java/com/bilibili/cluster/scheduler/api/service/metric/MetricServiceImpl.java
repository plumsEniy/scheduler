package com.bilibili.cluster.scheduler.api.service.metric;

import cn.hutool.core.net.url.UrlBuilder;
import cn.hutool.http.HttpRequest;
import cn.hutool.json.JSONUtil;
import com.bilibili.cluster.scheduler.common.dto.metric.dto.MetricNodeInstance;
import com.bilibili.cluster.scheduler.common.dto.metric.dto.UpdateMetricDto;
import com.bilibili.cluster.scheduler.common.dto.metric.resp.QueryMetricInstanceListResp;
import com.bilibili.cluster.scheduler.common.enums.metric.MetricEnvEnum;
import com.bilibili.cluster.scheduler.common.response.BaseResp;
import com.bilibili.cluster.scheduler.common.utils.BaseRespUtil;
import lombok.extern.slf4j.Slf4j;
import org.asynchttpclient.AsyncCompletionHandler;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.asynchttpclient.Response;
import org.asynchttpclient.request.body.multipart.Part;
import org.asynchttpclient.request.body.multipart.StringPart;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Future;

/**
 * @description: 监控服务
 * @Date: 2024/8/30 11:49
 * @Author: nizhiqiang
 */

@Service
@Slf4j
public class MetricServiceImpl implements MetricService {

    private static String TOKEN_KEY = "X-Authorization-Token";

    @Override
    public void addMetricInstance(MetricEnvEnum env, UpdateMetricDto updateMonitor, String token) {
        String url = UrlBuilder.ofHttp(env.getUrl())
                .addPath("/v1/open/instances/upsert")
                .build();

        String jsonBody = JSONUtil.toJsonStr(updateMonitor);
        log.info("sendIncrementNode url " + url + "sendIncrementNode body " + jsonBody);

        String respStr = HttpRequest.post(url)
                .header("Content-type", "application/json")
                .header(TOKEN_KEY, token)
                .body(jsonBody)
                .execute()
                .body();

        log.info("sendIncrementNode res " + respStr);
        BaseResp resp = JSONUtil.toBean(respStr, BaseResp.class);
        BaseRespUtil.checkCommonResp(resp);
        return;
    }

    @Override
    public void delMetricInstance(MetricEnvEnum env, UpdateMetricDto updateMonitor, String token) {
        String url = UrlBuilder.ofHttp(env.getUrl())
                .addPath("/v1/open/instances/deleteAll")
                .build();

        String jsonBody = JSONUtil.toJsonStr(updateMonitor);
        log.info("send delete Node url " + url + "sendDelNode body " + jsonBody);

        String respStr = HttpRequest.post(url)
                .header("Content-type", "application/json")
                .header(TOKEN_KEY, token)
                .body(jsonBody)
                .execute()
                .body();

        log.info("sendDelNode res " + respStr);
        BaseResp resp = JSONUtil.toBean(respStr, BaseResp.class);
        BaseRespUtil.checkCommonResp(resp);
        return;
    }

    @Override
    public void fullUpdateMetricInstance(MetricEnvEnum env, UpdateMetricDto updateMonitor, String token) {
        String url = UrlBuilder.ofHttp(env.getUrl())
                .addPath("/v1/open/instances/full")
                .build();

        String jsonBody = JSONUtil.toJsonStr(updateMonitor);
        log.info("full Update Node url " + url + "full Update Node body " + jsonBody);

        String respStr = HttpRequest.post(url)
                .header("Content-type", "application/json")
                .header(TOKEN_KEY, token)
                .body(jsonBody)
                .execute()
                .body();

        log.info("full Update Node res " + respStr);
        BaseResp resp = JSONUtil.toBean(respStr, BaseResp.class);
        BaseRespUtil.checkCommonResp(resp);
        return;
    }

    @Override
    public List<MetricNodeInstance> queryMetricInstanceList(MetricEnvEnum env, Integer integrationId) {
        String url = UrlBuilder.ofHttp(env.getUrl())
                .addPath("/v1/open/instances/list")
                .build();

        List<Part> parts = new ArrayList<>();
        parts.add(new StringPart("id", String.valueOf(integrationId), "multipart/form-data"));

        final String respStr = asyncRequest(url,
                parts);

        QueryMetricInstanceListResp resp = JSONUtil.toBean(respStr, QueryMetricInstanceListResp.class);
        BaseRespUtil.checkCommonResp(resp);
        List<MetricNodeInstance> result = resp.getData();
        if (CollectionUtils.isEmpty(result)) {
            return Collections.emptyList();
        }
        return result;
    }

    private String asyncRequest(String url, List<Part> params) {
        AsyncHttpClient c = new DefaultAsyncHttpClient();
        Future<Response> f = c.prepareGet(url).setBodyParts(
                        params)
                // .addHeader("authorization",TokenManager.getToken())
                .execute(new AsyncCompletionHandler<Response>() {

                    @Override
                    public Response onCompleted(Response response) {
                        return response;
                    }

                    @Override
                    public void onThrowable(Throwable t) {
                        throw new RuntimeException("exception");
                    }
                });
        Response response = null;
        try {
            long startTime = System.currentTimeMillis();
            while (true) {
                response = f.get();
                if (response != null) {
                    break;
                } else {
                    long endTime = System.currentTimeMillis();
                    // 大于20秒认为查询数据失败
                    if ((endTime - startTime) / 1000 > 40) {
                        throw new RuntimeException("exception");
                    }
                }
            }
        } catch (Exception e) {
            log.error(String.format("查询url失败,url为%s,参数为%s,异常为%s", url, params, e.getMessage()), e);
        }
        return response == null ? "" : response.getResponseBody();
    }
}
