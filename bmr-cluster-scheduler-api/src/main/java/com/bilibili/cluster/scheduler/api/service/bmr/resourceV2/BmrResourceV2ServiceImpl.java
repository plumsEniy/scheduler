package com.bilibili.cluster.scheduler.api.service.bmr.resourceV2;

import cn.hutool.core.net.url.UrlBuilder;
import cn.hutool.http.HttpRequest;
import cn.hutool.json.JSONUtil;
import com.bilibili.cluster.scheduler.common.Constants;
import com.bilibili.cluster.scheduler.common.dto.bmr.resource.req.NodeStateUpdateReq;
import com.bilibili.cluster.scheduler.common.dto.bmr.resource.req.RefreshNodeListReq;
import com.bilibili.cluster.scheduler.common.dto.bmr.resourceV2.PageInfo;
import com.bilibili.cluster.scheduler.common.dto.bmr.resourceV2.RmsHostInfo;
import com.bilibili.cluster.scheduler.common.dto.bmr.resourceV2.TideNodeDetail;
import com.bilibili.cluster.scheduler.common.dto.bmr.resourceV2.model.ComponentHostRelationModel;
import com.bilibili.cluster.scheduler.common.dto.bmr.resourceV2.model.ResourceHostInfo;
import com.bilibili.cluster.scheduler.common.dto.bmr.resourceV2.model.ResourceLogicGroup;
import com.bilibili.cluster.scheduler.common.dto.bmr.resourceV2.req.QueryComponentHostPageReq;
import com.bilibili.cluster.scheduler.common.dto.bmr.resourceV2.req.QueryHostInfoReq;
import com.bilibili.cluster.scheduler.common.dto.bmr.resourceV2.req.QueryLogicGroupInfoReq;
import com.bilibili.cluster.scheduler.common.dto.bmr.resourceV2.resp.QueryComponentHostPageResp;
import com.bilibili.cluster.scheduler.common.dto.bmr.resourceV2.resp.QueryHostInfoResp;
import com.bilibili.cluster.scheduler.common.dto.bmr.resourceV2.resp.QueryHostRmsResp;
import com.bilibili.cluster.scheduler.common.dto.bmr.resourceV2.resp.QueryIsHolidayResp;
import com.bilibili.cluster.scheduler.common.dto.bmr.resourceV2.resp.QueryLogicGroupInfoResp;
import com.bilibili.cluster.scheduler.common.dto.bmr.resourceV2.resp.QueryTideNodeDetailResp;
import com.bilibili.cluster.scheduler.common.dto.bmr.resourceV2.resp.QueryTideNodeListResp;
import com.bilibili.cluster.scheduler.common.dto.tide.req.DynamicScalingQueryListPageReq;
import com.bilibili.cluster.scheduler.common.dto.tide.resp.DynamicScalingConfDTO;
import com.bilibili.cluster.scheduler.common.dto.tide.resp.DynamicScalingQueryListPageResp;
import com.bilibili.cluster.scheduler.common.dto.tide.type.DynamicScalingStrategy;
import com.bilibili.cluster.scheduler.common.enums.resourceV2.TideClusterType;
import com.bilibili.cluster.scheduler.common.enums.resourceV2.TideNodeStatus;
import com.bilibili.cluster.scheduler.common.http.OkHttpUtils;
import com.bilibili.cluster.scheduler.common.response.BaseMsgResp;
import com.bilibili.cluster.scheduler.common.utils.BaseRespUtil;
import lombok.extern.slf4j.Slf4j;
import okhttp3.HttpUrl;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * @description: bmr的资源管理系统v2
 * @Date: 2024/9/4 16:07
 * @Author: nizhiqiang
 */

@Slf4j
@Service
public class BmrResourceV2ServiceImpl implements BmrResourceV2Service {

    @Value("${bmr.base-url:http://uat-cloud-bm.bilibili.co}")
    private String BASE_URL = "http://pre-cloud-bm.bilibili.co";

    @Override
    public List<ComponentHostRelationModel> queryComponentHostList(QueryComponentHostPageReq req) {

        int pageNum = 1;
        int pageSize = Constants.PAGE_MAX;
        List<ComponentHostRelationModel> resourceComponentHostRelationList = new ArrayList<>();

        while (true) {
            req.setPageNum(pageNum);
            req.setPageSize(pageSize);
            String url = UrlBuilder.ofHttp(BASE_URL)
                    .addPath("/resource/app/api/resource/component/host/get_list")
                    .build();

            log.info("query component host list page, req:{}", req);
            String respStr = HttpRequest.post(url)
                    .header(Constants.BMR_SYSTEM_INVOKER_KEY, Constants.BMR_SYSTEM_TOKEN)
                    .header(Constants.CONTENT_TYPE, Constants.APPLICATION_JSON)
                    .body(JSONUtil.toJsonStr(req))
                    .execute().body();

            log.info("query component host list page, resp:{}", respStr);
            QueryComponentHostPageResp resp = JSONUtil.toBean(respStr, QueryComponentHostPageResp.class);
            BaseRespUtil.checkMsgResp(resp);
            PageInfo<ComponentHostRelationModel> page = resp.getObj();

            List<ComponentHostRelationModel> currentComponentHostRelationList = page.getList();
            resourceComponentHostRelationList.addAll(currentComponentHostRelationList);
            if (CollectionUtils.isEmpty(currentComponentHostRelationList)) {
                log.info("query page finish, req is {}", JSONUtil.toJsonStr(req));
                break;
            }
            pageNum++;
        }
        return resourceComponentHostRelationList;
    }


    @Override
    public Boolean refreshDeployNodeInfo(RefreshNodeListReq refreshNodeListReq) {
        String url = UrlBuilder.ofHttp(BASE_URL)
                .addPath("/resource/app/api/resource/component/host/refresh")
                .build();
        String repStr = HttpRequest.post(url)
                .header(Constants.BMR_SYSTEM_INVOKER_KEY, Constants.BMR_SYSTEM_TOKEN)
                .body(JSONUtil.toJsonStr(refreshNodeListReq))
                .execute().body();
        BaseMsgResp resp = JSONUtil.toBean(repStr, BaseMsgResp.class);
        log.info("refresh deploy node info, req:{}", refreshNodeListReq);
        log.info("refresh deploy node info, resp:{}", resp);
        BaseRespUtil.checkMsgResp(resp);
        return true;
    }


    @Override
    public List<ResourceHostInfo> queryHostInfoByName(List<String> hostList) {
        QueryHostInfoReq req = new QueryHostInfoReq();
        req.setHostNameList(hostList);
        req.setPageNum(1);
        req.setPageSize(hostList.size() + 1);
        String url = UrlBuilder.ofHttp(BASE_URL)
                .addPath("/resource/app/api/resource/host/info/query/host/page")
                .build();
        String respStr = HttpRequest.post(url)
                .header(Constants.BMR_SYSTEM_INVOKER_KEY, Constants.BMR_SYSTEM_TOKEN)
                .body(JSONUtil.toJsonStr(req))
                .execute().body();

        QueryHostInfoResp resp = JSONUtil.toBean(respStr, QueryHostInfoResp.class);
        BaseRespUtil.checkMsgResp(resp);
        PageInfo<ResourceHostInfo> page = resp.getObj();
        List<ResourceHostInfo> hostInfoList = page.getList();

        return hostInfoList;
    }

    @Override
    public List<ResourceLogicGroup> queryLogicGroupList(QueryLogicGroupInfoReq req) {
        String url = UrlBuilder.ofHttp(BASE_URL)
                .addPath("/resource/app/api/sys/logic/group/all/list")
                .build();
        String repStr = HttpRequest.post(url)
                .header(Constants.BMR_SYSTEM_INVOKER_KEY, Constants.BMR_SYSTEM_TOKEN)
                .body(JSONUtil.toJsonStr(req))
                .execute().body();
        QueryLogicGroupInfoResp resp = JSONUtil.toBean(repStr, QueryLogicGroupInfoResp.class);
        BaseRespUtil.checkMsgResp(resp);
        return resp.getObj();
    }

    @Override
    public List<TideNodeDetail> queryTideOnBizUsedNodes(String appId, TideNodeStatus tideNodeStatus, TideClusterType tideClusterType) {
        String path = "/resource/app/api/tide/query-offline-node";
        String url = UrlBuilder.ofHttp(BASE_URL)
                .addPath(path)
                .addQuery("appId", appId)
                .addQuery("casterStatus", tideNodeStatus.name())
                .addQuery("belong", tideClusterType.name())
                .build();
        log.info("queryPrestoOnBizUsedNodes url is {}", url);

        String repStr = HttpRequest.get(url)
                .header(Constants.BMR_SYSTEM_INVOKER_KEY, Constants.BMR_SYSTEM_TOKEN)
                .execute().body();

        QueryTideNodeListResp resp = JSONUtil.toBean(repStr, QueryTideNodeListResp.class);
        BaseRespUtil.checkMsgResp(resp);
        return resp.getObj();
    }

    @Override
    public boolean updateTideNodeServiceAndStatus(String hostname, TideNodeStatus nodeStatus,
                                                  String appId, String deployService, TideClusterType belongResourcePool) {
        String path = "/resource/app/api/tide/update-node-status";

        final NodeStateUpdateReq nodeStateUpdateReq = new NodeStateUpdateReq();
        nodeStateUpdateReq.setAppId(appId);
        nodeStateUpdateReq.setHostName(hostname);
        nodeStateUpdateReq.setDeployService(deployService);
        nodeStateUpdateReq.setCasterStatus(nodeStatus.name());
        nodeStateUpdateReq.setBelongResourcePool(belongResourcePool);

        String body = JSONUtil.toJsonStr(nodeStateUpdateReq);
        String url = UrlBuilder.ofHttp(BASE_URL)
                .addPath(path).build();
        log.info("presto tide updateNodeServiceAndStatus url is {}, body is {}", url, body);

        try {
            String repStr = HttpRequest.post(url)
                    .header(Constants.BMR_SYSTEM_INVOKER_KEY, Constants.BMR_SYSTEM_TOKEN)
                    .body(body)
                    .execute().body();
            BaseMsgResp resp = JSONUtil.toBean(repStr, BaseMsgResp.class);
            BaseRespUtil.checkMsgResp(resp);
            return true;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return false;
        }
    }

    @Override
    public TideNodeDetail queryTideNodeDetail(String hostname) {
        String path = "/resource/app/api/tide/node/" + hostname;
        String url = UrlBuilder.ofHttp(BASE_URL)
                .addPath(path)
                .build();

        String repStr = null;
        try {
            repStr = HttpRequest.get(url)
                    .header(Constants.BMR_SYSTEM_INVOKER_KEY, Constants.BMR_SYSTEM_TOKEN)
                    .execute().body();
            QueryTideNodeDetailResp resp = JSONUtil.toBean(repStr, QueryTideNodeDetailResp.class);
            BaseRespUtil.checkMsgResp(resp);
            return resp.getObj();
        } catch (Exception e) {
            log.error("queryTideNodeDetail url is {}, resp is {}", url, repStr);
            log.error(e.getMessage(), e);
            return null;
        }
    }

    @Override
    public RmsHostInfo queryHostRmsInfo(String hostname) {
        String url = UrlBuilder.ofHttp(BASE_URL)
                .addPath("/resource/app/api/resource/host/info/query/rms/host")
                .addQuery("hostName", hostname)
                .build();

        String repStr = HttpRequest.get(url)
                .header(Constants.BMR_SYSTEM_INVOKER_KEY, Constants.BMR_SYSTEM_TOKEN)
                .execute().body();

        log.info("queryHostRmsInfo url is {}, repStr is {}", url, repStr);
        QueryHostRmsResp resp = JSONUtil.toBean(repStr, QueryHostRmsResp.class);
        BaseRespUtil.checkMsgResp(resp);
        return resp.getObj();
    }

    public List<DynamicScalingConfDTO> queryDynamicScalingConfList(DynamicScalingQueryListPageReq req) {
        String path = "/resource/app/api/dynamic-scaling/list";
        final HttpUrl httpUrl = HttpUrl.parse(BASE_URL).newBuilder().encodedPath(path).build();
        String body = JSONUtil.toJsonStr(req);
        log.info("queryDynamicScalingConfList url is {}, body is {}", httpUrl, body);
        final RequestBody requestBody = RequestBody.create(OkHttpUtils.getDefaultJsonMediaType(), body);
        final Request request = new Request.Builder().url(httpUrl)
                .header(Constants.BMR_SYSTEM_INVOKER_KEY, Constants.BMR_SYSTEM_TOKEN)
                .post(requestBody)
                .build();
        final String respStr = OkHttpUtils.execRequest(request, "queryDynamicScalingConfList");

        log.info("query component host list page, resp:{}", respStr);
        DynamicScalingQueryListPageResp resp = JSONUtil.toBean(respStr, DynamicScalingQueryListPageResp.class);
        BaseRespUtil.checkMsgResp(resp);
        return resp.getObj().getList();
    }

    @Override
    public long queryCurrentYarnTideClusterId(TideClusterType clusterType) {
        final DynamicScalingQueryListPageReq req = DynamicScalingQueryListPageReq.defaultReq(clusterType);
        final List<DynamicScalingConfDTO> scalingConfDTOS = queryDynamicScalingConfList(req);
        if (CollectionUtils.isEmpty(scalingConfDTOS)) {
            return 0l;
        }

        for (DynamicScalingConfDTO scalingConfDTO : scalingConfDTOS) {
            // 没启用潮汐调度
            if (!scalingConfDTO.isScalingState()) {
                continue;
            }
            // 必须是yarn潮汐任务
            final DynamicScalingStrategy dynamicScalingStrategy = scalingConfDTO.getDynamicScalingStrategy();
            if (dynamicScalingStrategy != DynamicScalingStrategy.YARN_TIDAL_SCHEDULE) {
                continue;
            }
            return scalingConfDTO.getClusterId();
        }
        // not find
        return 0;
    }

    @Override
    public boolean isHoliday(String date) {
        try {
            String path = "/resource/app/api/query/holiday/info";
            final HttpUrl httpUrl = HttpUrl.parse(BASE_URL).newBuilder().encodedPath(path)
                    .addEncodedQueryParameter("date", date)
                    .build();

            final Request request = new Request.Builder().url(httpUrl)
                    .header(Constants.BMR_SYSTEM_INVOKER_KEY, Constants.BMR_SYSTEM_TOKEN)
                    .get().build();

            final String respStr = OkHttpUtils.execRequest(request, "query isHoliday api");

            QueryIsHolidayResp resp = JSONUtil.toBean(respStr, QueryIsHolidayResp.class);
            BaseRespUtil.checkMsgResp(resp);
            return resp.getObj();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return false;
        }
    }

}
