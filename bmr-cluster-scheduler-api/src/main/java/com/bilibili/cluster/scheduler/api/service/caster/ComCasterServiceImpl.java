package com.bilibili.cluster.scheduler.api.service.caster;

import cn.hutool.core.net.url.UrlBuilder;
import cn.hutool.http.Header;
import cn.hutool.http.HttpRequest;
import cn.hutool.json.JSONUtil;
import com.bilibili.cluster.scheduler.common.Constants;
import com.bilibili.cluster.scheduler.common.dto.caster.*;
import com.bilibili.cluster.scheduler.common.dto.caster.req.CasterNodeTaintReq;
import com.bilibili.cluster.scheduler.common.dto.caster.req.DeletePvcReq;
import com.bilibili.cluster.scheduler.common.dto.caster.req.RemoveK8sNodeLabelReq;
import com.bilibili.cluster.scheduler.common.dto.caster.resp.*;
import com.bilibili.cluster.scheduler.common.enums.resourceV2.TideClusterType;
import com.bilibili.cluster.scheduler.common.utils.BaseRespUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ComCasterServiceImpl implements ComCasterService {

    @Value("${com-caster.url}")
    private String comCasterUrl = "http://com.caster.bilibili.co";

    @Value("${com-caster.api-token}")
    private String apiToken = "b21lZ2EtYm1ycGxhdGZvcm0tY21kYg==";

    @Value("${com-caster.platform-id}")
    private String platformId = "omega-bmrplatform-cmdb";


    @Override
    public String authPlatform(String apiToken, String platformId) {
        try {
            String url = comCasterUrl + "/api/com/auth/platform";
            Map<String, String> params = new HashMap<>();
            params.put("api_token", apiToken);
            params.put("platform_id", platformId);
            String resp = HttpRequest.post(url).header(Header.CONTENT_TYPE, "application/json")
                    .timeout(20_000)
                    .body(JSONUtil.toJsonStr(params))
                    .execute().body();

            log.info("/api/com/auth/platform resp:{}", resp);

            AuthPlatformResp authPlatformResp = JSONUtil.toBean(resp, AuthPlatformResp.class);
            BaseRespUtil.checkComResp(authPlatformResp);
            String token = authPlatformResp.getData().getToken();
            log.info("caster token is {}", token);

            return token;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return null;
    }

    @Override
    public boolean removeK8sLabel(Integer clusterId, List<String> ipList) {
        String url = comCasterUrl + "/api/com/nodes/label";
        String token = authPlatform(apiToken, platformId);

        RemoveK8sNodeLabelReq req = new RemoveK8sNodeLabelReq(clusterId, ipList.toArray(new String[]{}), Constants.K8S_LABEL_KEY);
        log.info("remove k8s node label, req:{}", req);
        String respStr = HttpRequest.delete(url)
                .header(Constants.CASTER_TOKEN_KEY, token)
                .timeout(20_000)
                .header(Constants.CONTENT_TYPE, Constants.APPLICATION_JSON)
                .body(JSONUtil.toJsonStr(req))
                .execute().body();

        log.info("/api/com/nodes/label resp:{}", respStr);
        RemoveK8sNodeLabelResp resp = JSONUtil.toBean(respStr, RemoveK8sNodeLabelResp.class);

        BaseRespUtil.checkComResp(resp);
        return true;
    }

    @Override
    public List<PodInfo> queryPodList(long clusterId, String podselector, String hostnames, String namespace) {
        String token = authPlatform(apiToken, platformId);
        UrlBuilder urlBuilder = UrlBuilder.ofHttp(comCasterUrl)
                .addPath("/api/com/pods")
                .addQuery("cluster_id", String.valueOf(clusterId));

        if (!StringUtils.isEmpty(podselector)) {
            urlBuilder.addQuery("podselector", podselector);
        }

        if (!StringUtils.isEmpty(hostnames)) {
            urlBuilder.addQuery("hostnames", hostnames);
        }

        if (!StringUtils.isEmpty(namespace)) {
            urlBuilder.addQuery("namespace", namespace);
        }

        String url = urlBuilder.build();

        log.info("query pod url is {}", url);
        String respStr = HttpRequest.get(url)
                .header(Constants.CASTER_TOKEN_KEY, token)
                .execute()
                .body();
        log.info("query pod resp is {}", respStr);

        QueryPodInfoResp resp = JSONUtil.toBean(respStr, QueryPodInfoResp.class);
        BaseRespUtil.checkComResp(resp);

        return resp.getData();
    }

    @Override
    public List<PodInfo> queryPodList(long clusterId, Map<String, Object> labelMap, String hostnames, String namespace) {
        String podSelector = Constants.EMPTY_STRING;
        if (!CollectionUtils.isEmpty(labelMap)) {
            podSelector = labelMap.entrySet().stream()
                    .map(entry -> entry.getKey() + "=" + entry.getValue().toString())
                    .collect(Collectors.joining(Constants.COMMA));
        }
        return queryPodList(clusterId, podSelector, hostnames, namespace);
    }

    @Override
    public List<ResourceNodeInfo> queryAllNodeInfo(long clusterId) {
        String path = "/api/com/nodes";
        String token = authPlatform(apiToken, platformId);
        UrlBuilder urlBuilder = UrlBuilder.ofHttp(comCasterUrl)
                .addPath(path)
                .addQuery("cluster_id", String.valueOf(clusterId))
                .addQuery("with_pod_info", "0");
        String url = urlBuilder.build();
        log.info("com caster queryAllNodeInfo url is {}", url);
        String respStr = HttpRequest.get(url)
                .header(Constants.CASTER_TOKEN_KEY, token)
                .execute()
                .body();
        QueryNodeInfoListResp resp = JSONUtil.toBean(respStr, QueryNodeInfoListResp.class);
        BaseRespUtil.checkComResp(resp);

        return resp.getData().getItems();
    }

    @Override
    public List<PodInfo> queryPodListByNodeIp(long clusterId, String nodeIp) {
        String path = "/api/com/node/desc";
        String token = authPlatform(apiToken, platformId);
        UrlBuilder urlBuilder = UrlBuilder.ofHttp(comCasterUrl)
                .addPath(path)
                .addQuery("cluster_id", String.valueOf(clusterId))
                .addQuery("nodename", nodeIp);
        String url = urlBuilder.build();
        log.info("com caster queryPodListByNodeName url is {}", url);
        String respStr = HttpRequest.get(url)
                .header(Constants.CASTER_TOKEN_KEY, token)
                .execute()
                .body();

        QueryPodInfoListByNodeResp resp = JSONUtil.toBean(respStr, QueryPodInfoListByNodeResp.class);
        BaseRespUtil.checkComResp(resp);

        return resp.getData().getPods().getPodsResource();
    }

    @Override
    public boolean updateNodeToTaintOn(long clusterId, String nodeName, TideClusterType tideClusterType) {
        try {
            String path = "/api/com/nodes/taint_node";
            final CasterNodeTaintReq casterNodeTaintReq = new CasterNodeTaintReq();
            casterNodeTaintReq.setClusterId(clusterId);
            final NodeTaint nodeTaint = new NodeTaint();
            nodeTaint.setNodeName(nodeName);
            final TaintOption taintOption = new TaintOption();
            taintOption.setOperation(TaintOperation.add);
            taintOption.setTaint(new TaintConf(tideClusterType));
            nodeTaint.setTaintOptions(Arrays.asList(taintOption));
            casterNodeTaintReq.setNodeTaintList(Arrays.asList(nodeTaint));

            String body = JSONUtil.toJsonStr(casterNodeTaintReq);
            String token = authPlatform(apiToken, platformId);
            String url = UrlBuilder.ofHttp(comCasterUrl).addPath(path).build();
            log.info("com caster updateNodeToTaintOn url is {}, body is {}", url, body);

            String respStr = HttpRequest.post(url)
                    .header(Constants.CASTER_TOKEN_KEY, token)
                    .body(body)
                    .execute().body();

            log.info("com caster updateNodeToTaintOn resp is {}.", respStr);
            BaseComResp resp = JSONUtil.toBean(respStr, BaseComResp.class);
            BaseRespUtil.checkComResp(resp);
        } catch (Exception e) {
            log.error("com caster updateNodeToTaintOn error, clusterId is {}, nodeName is [}", clusterId, nodeName);
            log.error(e.getMessage(), e);
            return false;
        }
        return true;
    }

    @Override
    public boolean updateNodeToTaintOff(long clusterId, String nodeName, TideClusterType tideClusterType) {
        try {
            String path = "/api/com/nodes/taint_node";
            final CasterNodeTaintReq casterNodeTaintReq = new CasterNodeTaintReq();
            casterNodeTaintReq.setClusterId(clusterId);
            final NodeTaint nodeTaint = new NodeTaint();
            nodeTaint.setNodeName(nodeName);
            final TaintOption taintOption = new TaintOption();
            taintOption.setOperation(TaintOperation.delete);
            taintOption.setTaint(new TaintConf(tideClusterType));
            nodeTaint.setTaintOptions(Arrays.asList(taintOption));
            casterNodeTaintReq.setNodeTaintList(Arrays.asList(nodeTaint));

            String body = JSONUtil.toJsonStr(casterNodeTaintReq);
            String token = authPlatform(apiToken, platformId);
            String url = UrlBuilder.ofHttp(comCasterUrl).addPath(path).build();
            log.info("com caster updateNodeToTaintOff url is {}, body is {}", url, body);

            String respStr = HttpRequest.post(url)
                    .header(Constants.CASTER_TOKEN_KEY, token)
                    .body(body)
                    .execute().body();

            log.info("com caster updateNodeToTaintOff resp is {}.", respStr);

            BaseComResp resp = JSONUtil.toBean(respStr, BaseComResp.class);
            BaseRespUtil.checkComResp(resp);
        } catch (Exception e) {
            log.error("com caster updateNodeToTaintOff error, clusterId is {}, nodeName is {}", clusterId, nodeName);
            log.error(e.getMessage(), e);
            return false;
        }
        return true;
    }

    @Override
    public void deletePvc(long clusterId, String namespace, List<String> pvcNames) {

        DeletePvcReq req = new DeletePvcReq();
        req.setClusterId(clusterId);
        req.setNamespace(namespace);
        req.setPvcNames(pvcNames);
        String token = authPlatform(apiToken, platformId);

        String url = UrlBuilder.of(comCasterUrl)
                .addPath("/api/com/pvcs/delete")
                .build();

        log.info("com caster deletePvc url is {}, body is {}", url, JSONUtil.toJsonStr(req));
        String respStr = HttpRequest.post(url)
                .header(Constants.CASTER_TOKEN_KEY, token)
                .body(JSONUtil.toJsonStr(req))
                .execute()
                .body();
        log.info("com caster deletePvc resp is {}", respStr);
        BaseComResp resp = JSONUtil.toBean(respStr, BaseComResp.class);
        BaseRespUtil.checkComResp(resp);
    }

    @Override
    public List<PvcInfo> queryPvcListByHost(long clusterId, String hostIp, String namespace, Map<String, Object> labelMap) {
        String path = "/api/com/pvcs/local/node";
        String token = authPlatform(apiToken, platformId);
        UrlBuilder urlBuilder = UrlBuilder.ofHttp(comCasterUrl)
                .addPath(path);
        urlBuilder.addQuery("cluster_id",String.valueOf(clusterId));
        urlBuilder.addQuery("node_ip",hostIp);

        if (!StringUtils.isBlank(namespace)) {
            urlBuilder.addQuery("namespace",namespace);
        }

        if (CollectionUtils.isEmpty(labelMap)) {
            String podSelector = Constants.EMPTY_STRING;
            if (!CollectionUtils.isEmpty(labelMap)) {
                podSelector = labelMap.entrySet().stream()
                        .map(entry -> entry.getKey() + "=" + entry.getValue().toString())
                        .collect(Collectors.joining(Constants.COMMA));
            }
            urlBuilder.addQuery("selector",podSelector);
        }


        String url = urlBuilder.build();

        log.info("query pvc by host url is {}", url);
        String respStr = HttpRequest.get(url)
                .header(Constants.CASTER_TOKEN_KEY, token)
                .execute()
                .body();

        log.info("query pvc by host resp is {}", respStr);

        QueryPvcByHostResp resp = JSONUtil.toBean(respStr, QueryPvcByHostResp.class);
        BaseRespUtil.checkComResp(resp);
        return resp.getData();
    }

}
