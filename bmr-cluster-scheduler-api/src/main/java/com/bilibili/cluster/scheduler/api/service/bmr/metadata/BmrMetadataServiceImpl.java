package com.bilibili.cluster.scheduler.api.service.bmr.metadata;

import cn.hutool.core.net.url.UrlBuilder;
import cn.hutool.http.HttpRequest;
import cn.hutool.json.JSONUtil;
import com.bilibili.cluster.scheduler.common.Constants;
import com.bilibili.cluster.scheduler.common.dto.bmr.metadata.*;
import com.bilibili.cluster.scheduler.common.dto.bmr.metadata.model.ComponentVariable;
import com.bilibili.cluster.scheduler.common.dto.bmr.metadata.req.QueryComponentListReq;
import com.bilibili.cluster.scheduler.common.dto.bmr.metadata.req.QueryInstallationPackageListReq;
import com.bilibili.cluster.scheduler.common.dto.bmr.metadata.resp.*;
import com.bilibili.cluster.scheduler.common.dto.spark.manager.resp.QueryFallbackPackageResp;
import com.bilibili.cluster.scheduler.common.exception.RequesterException;
import com.bilibili.cluster.scheduler.common.http.OkHttpUtils;
import com.bilibili.cluster.scheduler.common.response.BaseMsgResp;
import com.bilibili.cluster.scheduler.common.utils.BaseRespUtil;
import com.google.common.base.Preconditions;
import lombok.extern.slf4j.Slf4j;
import okhttp3.HttpUrl;
import okhttp3.Request;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @description: bmr的元数据管理系统
 * @Date: 2024/5/10 17:34
 * @Author: nizhiqiang
 */
@Slf4j
@Service
public class BmrMetadataServiceImpl implements BmrMetadataService {

    @Value("${bmr.base-url:http://uat-cloud-bm.bilibili.co}")
    private String BASE_URL = "http://pre-cloud-bm.bilibili.co";

    @Override
    public List<MetadataComponentData> queryComponentListByClusterId(long clusterId) {
        String url = UrlBuilder.of(BASE_URL)
                .addPath("/bmr-cluster-metadata/app/api/bmr/component/query/component/list")
                .build();

        log.info("query component list,cluster id is {}", clusterId);
        QueryComponentListReq req = new QueryComponentListReq();
        req.setClusterId(clusterId);
        String respStr = HttpRequest.post(url)
                .header(Constants.BMR_SYSTEM_INVOKER_KEY, Constants.BMR_SYSTEM_TOKEN)
                .body(JSONUtil.toJsonStr(req))
                .execute().body();
        QueryComponentListResp resp = JSONUtil.toBean(respStr, QueryComponentListResp.class);
        log.info("query component list,resp is {}", JSONUtil.toJsonStr(resp));
        BaseRespUtil.checkMsgResp(resp);
        return resp.getObj();
    }

    @Override
    public MetadataComponentData queryComponentByComponentId(long componentId) {
        String url = UrlBuilder.of(BASE_URL)
                .addPath("/bmr-cluster-metadata/app/api/bmr/component/query/component/one")
                .addQuery("id", String.valueOf(componentId))
                .build();

        String respStr = HttpRequest.get(url)
                .header(Constants.BMR_SYSTEM_INVOKER_KEY, Constants.BMR_SYSTEM_TOKEN)
                .execute().body();
        QueryMetadataComponentResp resp = JSONUtil.toBean(respStr, QueryMetadataComponentResp.class);
        BaseRespUtil.checkMsgResp(resp);
        return resp.getObj();
    }

    @Override
    public MetadataPackageData queryPackageDetailById(long packageId) {
        String url = UrlBuilder.of(BASE_URL)
                .addPath("/bmr-cluster-metadata/app/api/bmr/installationPackage/query/Package/one")
                .addQuery("id", String.valueOf(packageId))
                .build();
        String respStr = HttpRequest.post(url)
                .header(Constants.BMR_SYSTEM_INVOKER_KEY, Constants.BMR_SYSTEM_TOKEN)
                .execute().body();
        QueryPackageMsgResp resp = JSONUtil.toBean(respStr, QueryPackageMsgResp.class);
        BaseRespUtil.checkMsgResp(resp);
        return resp.getObj();
    }

    @Override
    public String queryPackageDownloadInfo(long packageId) {
        String url = UrlBuilder.of(BASE_URL)
                .addPath("/bmr-cluster-metadata/app/api/bmr/installationPackage/query/Package/boss/address")
                .addQuery("id", String.valueOf(packageId))
                .build();
        String respStr = HttpRequest.post(url)
                .header(Constants.BMR_SYSTEM_INVOKER_KEY, Constants.BMR_SYSTEM_TOKEN)
                .execute().body();
        QueryPackageDownloadResp resp = JSONUtil.toBean(respStr, QueryPackageDownloadResp.class);
        BaseRespUtil.checkMsgResp(resp);
        return resp.getObj();
    }

    @Override
    public Map<String, String> queryVariableByComponentId(long componentId) {

        String url = UrlBuilder.of(BASE_URL)
                .addPath("/bmr-cluster-metadata/app/api/bmr/component/query/variable/by/id")
                .addQuery("componentId", String.valueOf(componentId))
                .build();
        String respStr = HttpRequest.get(url)
                .header(Constants.BMR_SYSTEM_INVOKER_KEY, Constants.BMR_SYSTEM_TOKEN)
                .execute().body();
        QueryVariableByComponentIdResp resp = JSONUtil.toBean(respStr, QueryVariableByComponentIdResp.class);
        BaseRespUtil.checkMsgResp(resp);
        List<ComponentVariable> componentVariableList = resp.getObj();
        return componentVariableList.stream().collect(Collectors.toMap(ComponentVariable::getDictLabel, ComponentVariable::getDictValue));
    }

    @Override
    public MetadataClusterData queryClusterDetail(long clusterId) {
        String url = UrlBuilder.of(BASE_URL)
                .addPath("/bmr-cluster-metadata/app/api/bmr/cluster/query/yarn/cluster")
                .addQuery("req", String.valueOf(clusterId))
                .build();

        String respStr = HttpRequest.post(url)
                .header(Constants.BMR_SYSTEM_INVOKER_KEY, Constants.BMR_SYSTEM_TOKEN)
                .execute().body();

        QueryClusterDetailResp resp = JSONUtil.toBean(respStr, QueryClusterDetailResp.class);
        BaseRespUtil.checkMsgResp(resp);
        return resp.getObj();
    }

    @Override
    public List<MetadataMonitorConf> queryMonitorConfList(Long clusterId, Long componentId) {
        String url = UrlBuilder.of(BASE_URL)
                .addPath("/bmr-cluster-metadata/app/api/bmr/monitor/query/monitor/config")
                .addQuery("clusterId", String.valueOf(clusterId))
                .addQuery("componentId", String.valueOf(componentId))
                .build();
        log.info("queryMonitorConfList url is {}", url);

        String respStr = HttpRequest.get(url)
                .header(Constants.BMR_SYSTEM_INVOKER_KEY, Constants.BMR_SYSTEM_TOKEN)
                .execute().body();
        log.info("queryMonitorConfList resp is {}", respStr);

        QueryMonitorConfResp resp = JSONUtil.toBean(respStr, QueryMonitorConfResp.class);
        BaseRespUtil.checkMsgResp(resp);
        return resp.getObj();
    }

    @Override
    public long queryDefaultPackageIdByComponentId(Long componentId) {
        UrlBuilder urlBuilder = UrlBuilder.of(BASE_URL)
                .addPath("/bmr-cluster-metadata/app/api/bmr/installationPackage/query/package/default/version");
        if (componentId != null && componentId > 0) {
            urlBuilder.addQuery("componentId", String.valueOf(componentId));
        }

        String url = urlBuilder.build();
        log.info("query default package url is  {}", url);
        String respStr = HttpRequest.get(url)
                .header(Constants.BMR_SYSTEM_INVOKER_KEY, Constants.BMR_SYSTEM_TOKEN)
                .execute().body();
        log.info("query default package,resp is {}", respStr);
        QueryDefaultPackageResp resp = JSONUtil.toBean(respStr, QueryDefaultPackageResp.class);
        BaseRespUtil.checkMsgResp(resp);
        return resp.getObj();
    }

    @Override
    public void updateDefaultVersion(Long packageId, String componentName) {
        UrlBuilder urlBuilder = UrlBuilder.of(BASE_URL)
                .addPath("/bmr-cluster-metadata/app/api/bmr/installationPackage/update/package/default/version")
                .addQuery("packageId", String.valueOf(packageId));
        if (StringUtils.hasText(componentName)) {
            urlBuilder.addQuery("componentName", componentName);
        }

        String url = urlBuilder.build();
        log.info("update default package url is  {}", url);
        String respStr = HttpRequest.get(url)
                .header(Constants.BMR_SYSTEM_INVOKER_KEY, Constants.BMR_SYSTEM_TOKEN)
                .execute().body();
        log.info("update default package,resp is {}", respStr);
        BaseMsgResp resp = JSONUtil.toBean(respStr, BaseMsgResp.class);
        BaseRespUtil.checkMsgResp(resp);
    }

    @Override
    public InstallationPackage queryPackageByMinorVersion(String minorVersion) {
        String url = UrlBuilder.of(BASE_URL)
                .addPath("/bmr-cluster-metadata/app/api/bmr/installationPackage/query/package/by/minorVersion")
                .addQuery("minorVersion", minorVersion)
                .build();
        log.info("query package by minor version url is  {}", url);
        String respStr = HttpRequest.get(url)
                .header(Constants.BMR_SYSTEM_INVOKER_KEY, Constants.BMR_SYSTEM_TOKEN)
                .execute().body();
        log.info("query package by minor version resp is  {}", respStr);
        QueryInstallationPackageResp resp = JSONUtil.toBean(respStr, QueryInstallationPackageResp.class);
        BaseRespUtil.checkMsgResp(resp);
        return resp.getObj();
    }

    @Override
    public void removeDefaultPackage(String componentName) {
        String url = UrlBuilder.of(BASE_URL)
                .addPath("/bmr-cluster-metadata/app/api/bmr/installationPackage/remove/package/default/version")
                .addQuery("componentName", componentName)
                .build();
        log.info("remove default pacakge url is  {}", url);
        String respStr = HttpRequest.get(url)
                .header(Constants.BMR_SYSTEM_INVOKER_KEY, Constants.BMR_SYSTEM_TOKEN)
                .execute().body();
        log.info("remove default pacakge resp is  {}", respStr);
        BaseMsgResp resp = JSONUtil.toBean(respStr, BaseMsgResp.class);
        BaseRespUtil.checkMsgResp(resp);
    }

    @Override
    public InstallationPackage querySparkDefaultPackage() {
        String url = UrlBuilder.of(BASE_URL)
                .addPath("/bmr-cluster-metadata/app/api/bmr/installationPackage/query/spark/default/package")
                .build();
        log.info("query spark default package url is  {}", url);
        String respStr = HttpRequest.get(url)
                .header(Constants.BMR_SYSTEM_INVOKER_KEY, Constants.BMR_SYSTEM_TOKEN)
                .execute().body();
        log.info("query spark default package resp is  {}", respStr);
        QueryInstallationPackageResp resp = JSONUtil.toBean(respStr, QueryInstallationPackageResp.class);
        if (resp.getCode() == 1000) {
            log.warn("spark not set default package");
            return null;
        }
        if (resp.getCode() != 0) {
            log.error("request error : {}", resp);
            throw new RequesterException(resp == null ? "response is null!" : resp.getMsg());
        }
        return resp.getObj();
    }

    @Override
    public InstallationPackage querySparkPeripheryComponentDefaultPackage(String component) {
        final QueryFallbackPackageResp resp = querySparkComponentDefaultVersionResp(component);
        if (resp.getCode() == 0) {
            return resp.getObj();
        }
        return null;
    }

    public QueryFallbackPackageResp querySparkComponentDefaultVersionResp(String component) {
        String path = "/bmr-cluster-metadata/app/api/bmr/installationPackage/query/spark/periphery/component/default/version";
        final HttpUrl httpUrl = HttpUrl.parse(BASE_URL).newBuilder().encodedPath(path)
                .addEncodedQueryParameter("component", component)
                .build();
        log.info("query spark relation component default package url is {}", httpUrl);

        final Request request = new Request.Builder()
                .url(httpUrl)
                .header(Constants.BMR_SYSTEM_INVOKER_KEY, Constants.BMR_SYSTEM_TOKEN)
                .get()
                .build();

        String respJson = OkHttpUtils.execRequest(request, "query spark relation component default package");
        QueryFallbackPackageResp resp = JSONUtil.toBean(respJson, QueryFallbackPackageResp.class);
        Preconditions.checkNotNull(resp, "服务端异常，响应体为空");
        return resp;
    }

    @Override
    public List<String> queryWechatRobotByComponentId(long componentId) {
        if (componentId <= 0) {
            return Collections.emptyList();
        }

        String url = UrlBuilder.ofHttp(BASE_URL)
                .addPath("/bmr-cluster-metadata/app/api/bmr/robot/query/robot/list")
                .addQuery("componentId", String.valueOf(componentId))
                .build();

        String respStr = HttpRequest.get(url)
                .header(Constants.BMR_SYSTEM_INVOKER_KEY, Constants.BMR_SYSTEM_TOKEN)
                .execute().body();
        log.info("query wechat robot resp is  {}", respStr);
        QueryWechatRobotMsgResp resp = JSONUtil.toBean(respStr, QueryWechatRobotMsgResp.class);
        return resp.getObj();
    }

    @Override
    public List<InstallationPackage> queryInstallationPackageList(QueryInstallationPackageListReq req) {
        String url = UrlBuilder.ofHttp(BASE_URL)
                .addPath("/bmr-cluster-metadata/app/api/bmr/installationPackage/query/installationPackage/list")
                .build();

        String respStr = HttpRequest.post(url)
                .header(Constants.BMR_SYSTEM_INVOKER_KEY, Constants.BMR_SYSTEM_TOKEN)
                .body(JSONUtil.toJsonStr(req))
                .execute().body();

        log.info("queryInstallationPackageList,req is {}, resp is {}", JSONUtil.toJsonStr(req), respStr);
        QueryInstallationPackageListResp resp = JSONUtil.toBean(respStr, QueryInstallationPackageListResp.class);
        BaseRespUtil.checkMsgResp(resp);
        return resp.getObj().getList();
    }

}
