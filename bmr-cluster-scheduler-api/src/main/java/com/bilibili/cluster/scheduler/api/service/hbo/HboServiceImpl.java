package com.bilibili.cluster.scheduler.api.service.hbo;

import cn.hutool.core.net.url.UrlBuilder;
import cn.hutool.http.HttpRequest;
import cn.hutool.json.JSONUtil;
import com.bilibili.cluster.scheduler.common.Constants;
import com.bilibili.cluster.scheduler.common.dto.hbo.model.HboJob;
import com.bilibili.cluster.scheduler.common.dto.hbo.model.HboJobInfo;
import com.bilibili.cluster.scheduler.common.dto.hbo.model.UpdateHboJobParamsDto;
import com.bilibili.cluster.scheduler.common.dto.hbo.resp.QueryJobListResp;
import com.bilibili.cluster.scheduler.common.response.BaseMsgResp;
import com.bilibili.cluster.scheduler.common.utils.BaseRespUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

/**
 * hbo 操作类
 */
@Service
@Slf4j
public class HboServiceImpl implements HboService {

    @Value("${hbo.prefix}")
    private String BASE_URL;

    @Override
    public List<HboJobInfo> queryJobListByJobId(List<String> jobIdList) {
        StringJoiner joiner = new StringJoiner(Constants.COMMA);
        jobIdList.forEach(joiner::add);
        String url = UrlBuilder.ofHttp(BASE_URL)
                .addPath("api/statistics/jobParams/findByJobId")
                .addQuery("jobIdList", joiner.toString())
                .build();

        String respStr = HttpRequest.get(url)
                .header(Constants.CONTENT_TYPE, "application/json")
                .timeout(20000)// 超时，毫秒
                .execute().body();

        log.info("query job info list by job id resp is {}", respStr);
        QueryJobListResp resp = JSONUtil.toBean(respStr, QueryJobListResp.class);
        BaseRespUtil.checkHboResp(resp);
        return resp.getData();
    }

    @Override
    public void upsertJob(List<HboJob> jobList) {
        String url = UrlBuilder.ofHttp(BASE_URL)
                .addPath("/api/statistics/jobParams/upsert")
                .build();

        String respStr = HttpRequest.post(url)
                .header(Constants.CONTENT_TYPE, "application/json")
                .timeout(20000)// 超时，毫秒
                .body(JSONUtil.toJsonStr(jobList))
                .execute().body();

        log.info("upsert job resp is {}", respStr);
        BaseMsgResp resp = JSONUtil.toBean(respStr, BaseMsgResp.class);
        BaseRespUtil.checkHboResp(resp);
    }

    @Override
    public void deleteJob(List<String> jobIdList) {
        StringJoiner joiner = new StringJoiner(Constants.COMMA);
        jobIdList.forEach(joiner::add);
        String url = UrlBuilder.ofHttp(BASE_URL)
                .addPath("/api/statistics/jobParams/deleteByJobIdList")
                .addQuery("jobIdList", joiner.toString())
                .build();

        String respStr = HttpRequest.delete(url)
                .header(Constants.CONTENT_TYPE, "application/json")
                .timeout(20000)// 超时，毫秒
                .execute().body();
        log.info("delete job resp is {}", respStr);
        BaseMsgResp resp = JSONUtil.toBean(respStr, BaseMsgResp.class);
        BaseRespUtil.checkHboResp(resp);
    }

    @Override
    public void updateJobParams(List<String> jobIdList, Map<String, String> addParamsMap, Map<String, String> removeParamsMap) {

        if (CollectionUtils.isEmpty(addParamsMap) && CollectionUtils.isEmpty(removeParamsMap)) {
            throw new IllegalArgumentException("新增和删除参数不能都为空");
        }
        HashMap<String, UpdateHboJobParamsDto> jobIdToUpdateJobParamsMap = new HashMap<>();
        UpdateHboJobParamsDto updateHboJobParamsDto = new UpdateHboJobParamsDto();
        updateHboJobParamsDto.setAdd(addParamsMap);
        updateHboJobParamsDto.setRemove(removeParamsMap);

        StringJoiner joiner = new StringJoiner(Constants.COMMA);
        jobIdList.forEach(joiner::add);
        jobIdToUpdateJobParamsMap.put(joiner.toString(), updateHboJobParamsDto);

        String url = UrlBuilder.ofHttp(BASE_URL)
                .addPath("/api/statistics/jobParams/mergeJobParams")
                .build();
        String respStr = HttpRequest.post(url)
                .header(Constants.CONTENT_TYPE, "application/json")
                .timeout(20000)// 超时，毫秒
                .body(JSONUtil.toJsonStr(jobIdToUpdateJobParamsMap))
                .execute().body();
        log.info("update job param req is {},body is {},resp is {}", url, JSONUtil.toJsonStr(jobIdToUpdateJobParamsMap), respStr);
        BaseMsgResp resp = JSONUtil.toBean(respStr, BaseMsgResp.class);
        BaseRespUtil.checkHboResp(resp);
    }

}
