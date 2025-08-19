package com.bilibili.cluster.scheduler.api.service.bmr.resource;

import cn.hutool.core.net.url.UrlBuilder;
import cn.hutool.http.HttpRequest;
import cn.hutool.json.JSONUtil;
import com.bilibili.cluster.scheduler.common.Constants;
import com.bilibili.cluster.scheduler.common.dto.bmr.metadata.req.QueryNodeGroupInfoReq;
import com.bilibili.cluster.scheduler.common.dto.bmr.metadata.resp.QueryNodeGroupInfoResp;
import com.bilibili.cluster.scheduler.common.dto.bmr.resource.ComponentNodeDetail;
import com.bilibili.cluster.scheduler.common.dto.bmr.resource.model.HostAndLogicGroupInfo;
import com.bilibili.cluster.scheduler.common.dto.bmr.resource.model.ResourceHostInfo;
import com.bilibili.cluster.scheduler.common.dto.bmr.resource.req.*;
import com.bilibili.cluster.scheduler.common.dto.bmr.resource.resp.*;
import com.bilibili.cluster.scheduler.common.dto.yarn.RMInfoObj;
import com.bilibili.cluster.scheduler.common.dto.yarn.resp.QueryRMInfoResp;
import com.bilibili.cluster.scheduler.common.dto.zk.resp.QueryZkRoleResp;
import com.bilibili.cluster.scheduler.common.enums.flow.FlowDeployType;
import com.bilibili.cluster.scheduler.common.response.BaseMsgResp;
import com.bilibili.cluster.scheduler.common.utils.BaseRespUtil;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.*;

/**
 * @description: bmr资源管理系统
 * @Date: 2024/3/11 20:03
 * @Author: nizhiqiang
 */
@Slf4j
@Service
public class BmrResourceServiceImpl implements BmrResourceService {

    @Setter
    @Value("${bmr.base-url:http://uat-cloud-bm.bilibili.co}")
    private String BASE_URL = "http://uat-cloud-bm.bilibili.co";

    @Setter
    @Value("${spring.profiles.active}")
    private String active;

    @Override
    public boolean alterYarnLabel(List<String> hostNameList, String label) {
        if (active.equals(Constants.UAT_ENV)) {
            log.info("uat can not remove label");
            return true;
        }
        //        bmr正式的常熟实时集群id为11
        Long clusterId = 11L;

        String url = UrlBuilder.ofHttp(BASE_URL)
                .addPath("/bmr-cluster-resources/app/api/sys/label/add/node/label")
                .build();

        RemoveYarnLabelReq req = new RemoveYarnLabelReq(Constants.HOSTS_DEFAULT_LABEL, hostNameList, clusterId);

        log.info("remove yarn node label, req:{}", req);
        String respStr = HttpRequest.post(url)
                .header(Constants.BMR_SYSTEM_INVOKER_KEY, Constants.BMR_SYSTEM_TOKEN)
                .header(Constants.CONTENT_TYPE, Constants.APPLICATION_JSON)
                .body(JSONUtil.toJsonStr(req))
                .execute().body();

        log.info("remove yarn node label, resp:{}", respStr);
        RemoveYarnLabelResp resp = JSONUtil.toBean(respStr, RemoveYarnLabelResp.class);
        BaseRespUtil.checkMsgResp(resp);
        return true;
    }

    @Override
    public List<ComponentNodeDetail> queryNameNodeHostByClusterId(long clusterId) {
        String url = UrlBuilder.ofHttp(BASE_URL)
                .addPath("/bmr-cluster-resources/app/api/sys/host_info/find/namenode/hosts")
                .addQuery("clusterId", String.valueOf(clusterId))
                .build();
        String respStr = HttpRequest.get(url)
                .header(Constants.BMR_SYSTEM_INVOKER_KEY, Constants.BMR_SYSTEM_TOKEN)
                .header(Constants.CONTENT_TYPE, Constants.APPLICATION_JSON)
                .execute().body();
        QueryNameNodeHostByClusterIdResp resp = JSONUtil.toBean(respStr, QueryNameNodeHostByClusterIdResp.class);
        log.info("query name node host resp is :{}", respStr);
        BaseRespUtil.checkMsgResp(resp);
        return resp.getObj();
    }

    @Override
    public Map<String, HostAndLogicGroupInfo> queryNodeGroupInfo(long clusterId, List<String> nodeList) {
        String url = UrlBuilder.ofHttp(BASE_URL)
                .addPath("/bmr-cluster-resources/app/api/sys/logic_group/host_group")
                .build();

        QueryNodeGroupInfoReq req = new QueryNodeGroupInfoReq(clusterId, nodeList);

        String respStr = HttpRequest.post(url)
                .header(Constants.BMR_SYSTEM_INVOKER_KEY, Constants.BMR_SYSTEM_TOKEN)
                .header(Constants.CONTENT_TYPE, Constants.APPLICATION_JSON)
                .body(JSONUtil.toJsonStr(req))
                .execute().body();
        log.info("query node group info resp is :{}", respStr);
        QueryNodeGroupInfoResp resp = JSONUtil.toBean(respStr, QueryNodeGroupInfoResp.class);
        BaseRespUtil.checkMsgResp(resp);
        return resp.getObj();
    }

    @Override
    public List<String> filterJobAgentLiveness(List<String> nodeList) {
        if (CollectionUtils.isEmpty(nodeList)) {
            return Collections.emptyList();
        }

        String url = UrlBuilder.ofHttp(BASE_URL)
                .addPath("/bmr-cluster-resources/app/api/sys/host_info/job_agent_alive")
                .build();

        FilterJobAgentLivenessReq req = new FilterJobAgentLivenessReq(nodeList);
        String respStr = HttpRequest.post(url)
                .header(Constants.BMR_SYSTEM_INVOKER_KEY, Constants.BMR_SYSTEM_TOKEN)
                .header(Constants.CONTENT_TYPE, Constants.APPLICATION_JSON)
                .body(JSONUtil.toJsonStr(req))
                .execute().body();

        log.info("filter job agent liveness resp is :{}", respStr);

        FilterJobAgentLivenessResp resp = JSONUtil.toBean(respStr, FilterJobAgentLivenessResp.class);
        BaseRespUtil.checkMsgResp(resp);
        ArrayList<String> list = resp.getObj();
        if (CollectionUtils.isEmpty(list)) {
            return Collections.emptyList();
        }
        return list;
    }

    @Override
    public BaseMsgResp checkNameNodeSafeModeByClusterId(long clusterId) {
        String url = UrlBuilder.ofHttp(BASE_URL)
                .addPath("/bmr-cluster-resources/api/sys/component_host/exploratory/work/namenode/safeMode/by/clusterId")
                .addQuery("clusterId", String.valueOf(clusterId))
                .build();
        String repStr = HttpRequest.get(url)
                .header(Constants.BMR_SYSTEM_INVOKER_KEY, Constants.BMR_SYSTEM_TOKEN)
                .execute().body();
        BaseMsgResp baseMsgResp = JSONUtil.toBean(repStr, BaseMsgResp.class);
        return baseMsgResp;
    }

    @Override
    public List<ResourceHostInfo> queryHostListByName(List<String> hostnameList) {
        QueryHostInfoReq req = new QueryHostInfoReq();
        req.setHostNameList(hostnameList);
        req.setIsDiff(false);
        req.setPageNum(1);
        req.setPageSize(hostnameList.size());

        String url = UrlBuilder.ofHttp(BASE_URL)
                .addPath("/bmr-cluster-resources/app/api/sys/common/overall_host")
                .build();
        String respStr = HttpRequest.post(url)
                .header(Constants.BMR_SYSTEM_INVOKER_KEY, Constants.BMR_SYSTEM_TOKEN)
                .body(JSONUtil.toJsonStr(req))
                .execute().body();
        QueryHostInfoResp resp = JSONUtil.toBean(respStr, QueryHostInfoResp.class);
        BaseRespUtil.checkMsgResp(resp);
        return resp.getObj().getList();
    }

    @Override
    public Boolean updateNodeListState(long clusterId, long componentId
            , List<String> nodeList, FlowDeployType deployType
            , boolean success, String packageVersion, String configVersion) {
        if (!requireUpdateNodeStatus(deployType)) {
            return true;
        }
        String url = UrlBuilder.ofHttp(BASE_URL)
                .addPath("/bmr-cluster-resources/app/api/sys/host_info/component/refresh/hosts")
                .build();

        UpdateHostStateReq req = new UpdateHostStateReq(clusterId, componentId, nodeList, deployType,
                success, packageVersion, configVersion);
        String body = JSONUtil.toJsonStr(req);
        log.info("request to updateNodeListState, url is {}, request body is {}", url, body);
        String repStr = HttpRequest.post(url)
                .header(Constants.BMR_SYSTEM_INVOKER_KEY, Constants.BMR_SYSTEM_TOKEN)
                .body(body)
                .execute().body();
        BaseMsgResp resp = JSONUtil.toBean(repStr, BaseMsgResp.class);
        BaseRespUtil.checkMsgResp(resp);
        return true;
    }

    @Override
    public RMInfoObj queryRMComponentIdByClusterId(long yarnClusterId) {
        String path = "/bmr-cluster-resources/app/api/sys/host_info/find_rm_hosts";
        String url = UrlBuilder.ofHttp(BASE_URL).addPath(path)
                .addQuery("clusterId", String.valueOf(yarnClusterId))
                .build();
        String repStr = HttpRequest.get(url)
                .header(Constants.BMR_SYSTEM_INVOKER_KEY, Constants.BMR_SYSTEM_TOKEN)
                .execute().body();
        QueryRMInfoResp rmInfoResp = JSONUtil.toBean(repStr, QueryRMInfoResp.class);
        BaseRespUtil.checkMsgResp(rmInfoResp);
        return rmInfoResp.getObj();
    }

    @Override
    public List<ComponentNodeDetail> queryComponentNodeList(long clusterId, long componentId) {
        QueryComponentNodeListReq req = new QueryComponentNodeListReq(clusterId, componentId);
        return queryNodeList(req);
    }

    @Override
    public List<String> addHostToTideNodeGroup(long clusterId, List<String> hostList, String nodeGroupName) {
        if (CollectionUtils.isEmpty(hostList)) {
            return Collections.emptyList();
        }
        String path = "/bmr-cluster-resources/app/api/sys/logic_group/insert/host/by/group/name";
        final AddHostToNodeGroupReq req = new AddHostToNodeGroupReq(clusterId, hostList, nodeGroupName);
        String url = UrlBuilder.ofHttp(BASE_URL)
                .addPath(path)
                .build();
        String body = JSONUtil.toJsonStr(req);
        String repStr = HttpRequest.post(url)
                .header(Constants.BMR_SYSTEM_INVOKER_KEY, Constants.BMR_SYSTEM_TOKEN)
                .body(body)
                .timeout(15_000)
                .execute().body();
        AddHostToNodeGroupResp resp = JSONUtil.toBean(repStr, AddHostToNodeGroupResp.class);

        BaseRespUtil.checkMsgResp(resp);
        return resp.getObj().getNotExistHostList();
    }

    @Override
    public synchronized boolean switchYarnNodeLabel(long clusterId, String hostname, String nodeLabel) {
        final UpdateNodeLabelReq updateNodeLabelReq = new UpdateNodeLabelReq();
        updateNodeLabelReq.setClusterId(clusterId);
        updateNodeLabelReq.setNodes(Arrays.asList(hostname));
        updateNodeLabelReq.setLabel(nodeLabel);
        String path = "/bmr-cluster-resources/app/api/sys/label/add/node/label";
        String url = UrlBuilder.ofHttp(BASE_URL)
                .addPath(path)
                .build();
        String body = JSONUtil.toJsonStr(updateNodeLabelReq);
        log.info("bmr resource service switchYarnNodeLabel url is {}, body is {}", url, body);
        String repStr = HttpRequest.post(url)
                .header(Constants.BMR_SYSTEM_INVOKER_KEY, Constants.BMR_SYSTEM_TOKEN)
                .body(body)
                .execute().body();
        log.info("bmr resource service switchYarnNodeLabel resp is {}", repStr);

        BaseMsgResp resp = JSONUtil.toBean(repStr, BaseMsgResp.class);
        BaseRespUtil.checkMsgResp(resp);
        return true;
    }


    private boolean requireUpdateNodeStatus(FlowDeployType deployType) {
        if (Objects.isNull(deployType)) {
            return false;
        }
        switch (deployType) {
            case MODIFY_MONITOR_OBJECT:
            case SPARK_DEPLOY:
            case SPARK_DEPLOY_ROLLBACK:
            case SPARK_VERSION_LOCK:
            case SPARK_VERSION_RELEASE:
            case SPARK_EXPERIMENT:
            case SPARK_CLIENT_PACKAGE_DEPLOY:
                // 潮汐节点手动刷新状态
            case PRESTO_TIDE_OFF:
            case PRESTO_TIDE_ON:
            case NNPROXY_DEPLOY:
                return false;
            default:
                return true;
        }

    }

    @Override
    public List<ComponentNodeDetail> queryNodeList(QueryComponentNodeListReq req) {
        String path = "/bmr-cluster-resources/app/api/sys/component_host/get_list";
        String url = UrlBuilder.ofHttp(BASE_URL)
                .addPath(path)
                .build();
        String body = JSONUtil.toJsonStr(req);
        log.info("queryNodeList url is {}, body is {}", url, body);

        String repStr = HttpRequest.post(url)
                .header(Constants.BMR_SYSTEM_INVOKER_KEY, Constants.BMR_SYSTEM_TOKEN)
                .body(body)
                .execute().body();
        QueryComponentNodeListResp resp = JSONUtil.toBean(repStr, QueryComponentNodeListResp.class);

        BaseRespUtil.checkMsgResp(resp);
        return resp.getObj().getList();
    }

    @Override
    public String queryZkRole(String hostName) {
        String path = "/bmr-cluster-resources/app/api/sys/component_host/exploratory/work/zk/role";
        String url = UrlBuilder.ofHttp(BASE_URL).addPath(path)
                .addQuery("hostName", String.valueOf(hostName))
                .build();
        String repStr = HttpRequest.get(url)
                .header(Constants.BMR_SYSTEM_INVOKER_KEY, Constants.BMR_SYSTEM_TOKEN)
                .execute().body();
        QueryZkRoleResp resp = JSONUtil.toBean(repStr, QueryZkRoleResp.class);
        BaseRespUtil.checkMsgResp(resp);
        return resp.getObj();
    }

}
