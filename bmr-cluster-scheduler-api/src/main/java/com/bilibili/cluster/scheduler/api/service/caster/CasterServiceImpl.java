package com.bilibili.cluster.scheduler.api.service.caster;

import cn.hutool.core.net.url.UrlBuilder;
import cn.hutool.http.HttpRequest;
import cn.hutool.json.JSONUtil;
import com.bilibili.cluster.scheduler.common.Constants;
import com.bilibili.cluster.scheduler.common.dto.caster.resp.BaseComResp;
import com.bilibili.cluster.scheduler.common.dto.caster.resp.DeployK8sResp;
import com.bilibili.cluster.scheduler.common.dto.caster.resp.QueryLogResp;
import com.bilibili.cluster.scheduler.common.dto.clickhouse.clickhouse.ClickhouseDeployDTO;
import com.bilibili.cluster.scheduler.common.dto.presto.template.PrestoDeployDTO;
import com.bilibili.cluster.scheduler.common.utils.BaseRespUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Optional;

/**
 * @description:
 * @Date: 2024/8/8 17:34
 * @Author: nizhiqiang
 */

@Slf4j
@Service
public class CasterServiceImpl implements CasterService {

    @Value("${caster.base-url}")
    private String casterUrl;

    @Value("${com-caster.api-token}")
    private String apiToken;

    @Value("${com-caster.platform-id}")
    private String platformId;

    @Resource
    ComCasterService comCasterService;

    @Override
    public String deployPresto(PrestoDeployDTO prestoDeploy) {
        String url = UrlBuilder.ofHttp(casterUrl)
                .addPath("/api/v1/deployments/presto")
                .build();

        log.info("deploy presto url is {}", url);
        log.info("deploy presto req is {}", JSONUtil.toJsonStr(prestoDeploy));
        String token = comCasterService.authPlatform(apiToken, platformId);

        String respStr = HttpRequest.post(url)
                .header(Constants.CASTER_TOKEN_KEY, token)
                .body(JSONUtil.toJsonStr(prestoDeploy))
                .execute().body();

        log.info("deploy presto resp is {}", respStr);
        DeployK8sResp resp = JSONUtil.toBean(respStr, DeployK8sResp.class);
        BaseRespUtil.checkComResp(resp);
        String template = Optional.ofNullable(resp)
                .map(DeployK8sResp::getData)
                .map(DeployK8sResp.K8sRespData::getTemplate)
                .orElse(Constants.EMPTY_STRING);
        return template;
    }

    @Override
    public void deployClickHouse(ClickhouseDeployDTO clickhouseDeployDTO) {
        deployClickhouse(clickhouseDeployDTO);
    }

    private String deployClickhouse(ClickhouseDeployDTO clickhouseDeployDTO) {
        String url = UrlBuilder.ofHttp(casterUrl)
                .addPath("/api/v1/deployments/clickhouse")
                .build();

        log.info("deploy clickhouse url is {}, req is {}", url, JSONUtil.toJsonStr(clickhouseDeployDTO));
        String token = comCasterService.authPlatform(apiToken, platformId);
        String respStr = HttpRequest.post(url)
                .header(Constants.CASTER_TOKEN_KEY, token)
                .body(JSONUtil.toJsonStr(clickhouseDeployDTO))
                .execute().body();

        log.info("deploy clickhouse resp is {}", respStr);
        DeployK8sResp resp = JSONUtil.toBean(respStr, DeployK8sResp.class);
        BaseRespUtil.checkComResp(resp);
        String template = Optional.ofNullable(resp)
                .map(DeployK8sResp::getData)
                .map(DeployK8sResp.K8sRespData::getTemplate)
                .orElse(Constants.EMPTY_STRING);
        return template;
    }

    @Override
    public String getClickHouseTemplate(ClickhouseDeployDTO clickhouseDeployDTO) {
        clickhouseDeployDTO.setPreview(true);
        return deployClickhouse(clickhouseDeployDTO);
    }

    @Override
    public String queryLog(String podName, String appId, String env, String cluster, Integer tail, String container) {
        String[] splitAppIdList = appId.split("\\.");
        String url = UrlBuilder.ofHttp(casterUrl + "/api/v1/log/pod")
                .addPath(podName)
                .addQuery("bu", splitAppIdList[0])
                .addQuery("project", splitAppIdList[1])
                .addQuery("env", env)
                .addQuery("cluster", cluster)
                .addQuery("tail", String.valueOf(tail))
                .addQuery("container", container)
                .build();
        log.info("query log url is {}", url);

        String token = comCasterService.authPlatform(apiToken, platformId);

        String respStr = HttpRequest.get(url)
                .header(Constants.CASTER_TOKEN_KEY, token)
                .execute().body();


        log.info("query log resp is {}", respStr);
        QueryLogResp resp = JSONUtil.toBean(respStr, QueryLogResp.class);
        BaseRespUtil.checkComResp(resp);
        return resp.getData();
    }

    @Override
    public BaseComResp deletePresto(String appId, String env, String clusterName) {
        String url = UrlBuilder.ofHttp(casterUrl + "/api/v1/deployments/presto")
                .addQuery("app_id", appId)
                .addQuery("env", env)
                .addQuery("cluster_name", clusterName)
                .build();
        String token = comCasterService.authPlatform(apiToken, platformId);

        log.info("delete presto url is {}", url);
        String respStr = HttpRequest.delete(url)
                .header(Constants.CASTER_TOKEN_KEY, token)
                .execute().body();

        log.info("delete presto resp is {}", respStr);
        BaseComResp resp = JSONUtil.toBean(respStr, BaseComResp.class);
        return resp;
    }
}
