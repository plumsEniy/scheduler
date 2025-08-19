package com.bilibili.cluster.scheduler.api.service.jobAgent;

import cn.hutool.core.net.url.UrlBuilder;
import cn.hutool.http.HttpRequest;
import cn.hutool.json.JSONUtil;
import com.bilibili.cluster.scheduler.common.dto.jobAgent.JobAgentData;
import com.bilibili.cluster.scheduler.common.dto.jobAgent.TaskAtomDetail;
import com.bilibili.cluster.scheduler.common.dto.jobAgent.TaskAtomReport;
import com.bilibili.cluster.scheduler.common.dto.jobAgent.TaskSetData;
import com.bilibili.cluster.scheduler.common.dto.jobAgent.resp.GetTaskAtomListResp;
import com.bilibili.cluster.scheduler.common.dto.jobAgent.resp.GetTaskAtomResp;
import com.bilibili.cluster.scheduler.common.dto.jobAgent.resp.GetTaskSetResp;
import com.bilibili.cluster.scheduler.common.dto.jobAgent.resp.JobAgentCheckResponse;
import com.bilibili.cluster.scheduler.common.utils.BaseRespUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.Header;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @description: jobagent服务
 * @Date: 2024/5/10 10:51
 * @Author: nizhiqiang
 */
@Service
@Slf4j
public class JobAgentServiceImpl implements JobAgentService {

    private final String BASE_URL = "http://job.bilibili.co";
    private static final String SIGNATURE = "x-signature";
    private static final String SECRET_ID = "x-secretid";
    private static final String BILISPY_USER = "x1-bilispy-user";

    private Header[] authHeaders;

    private HttpRequest addHeaders(HttpRequest httpRequest) {
        return httpRequest.header(SIGNATURE, "2098a7d9962f0c7589f5db3caccd6fb9")
                .header(SECRET_ID, "ia2mo940YLgQZzg")
                .header(BILISPY_USER, "datacenter.alter.bmr-cluster-manager");
    }

    @Override
    public TaskSetData getTaskSetSummary(long taskSetId) {
        String url = UrlBuilder.ofHttp(BASE_URL)
                .addPath(String.format("/api/v3/relay/task_set/%s", taskSetId))
                .build();
        HttpRequest httpRequest = HttpRequest.get(url);
        httpRequest = addHeaders(httpRequest);
        String respStr = httpRequest.execute().body();
        GetTaskSetResp resp = JSONUtil.toBean(respStr, GetTaskSetResp.class);
        BaseRespUtil.checkJobAgentResp(resp);
        return resp.getData();
    }

    @Override
    public List<TaskAtomDetail> getTaskList(long taskSetId) {
        List<TaskAtomDetail> resultList = new ArrayList<>();
        int pageNum = 1;
        int pageSize = 20;

        while (true) {
            List<TaskAtomDetail> taskList = getTaskList(taskSetId, pageNum++, pageSize);
            if (CollectionUtils.isEmpty(taskList)) break;
            resultList.addAll(taskList);
            if (taskList.size() < pageSize) break;
        }
        log.info("query job agent, task set id is {}, task list is {}", taskSetId, JSONUtil.toJsonStr(resultList));
        return resultList;
    }

    @Override
    public TaskAtomReport getTaskReport(long taskId) {
        // https://job.bilibili.co/api/v3/relay/task_atom/437747843
        String url = UrlBuilder.ofHttp(BASE_URL)
                .addPath("/api/v3/relay/task_atom/" + taskId)
                .build();
        HttpRequest httpRequest = HttpRequest.get(url);
        httpRequest = addHeaders(httpRequest);
        String respStr = httpRequest.execute().body();
        GetTaskAtomResp taskAtomResp = JSONUtil.toBean(respStr, GetTaskAtomResp.class);
        BaseRespUtil.checkJobAgentResp(taskAtomResp);
        return taskAtomResp.getData();
    }

    public List<TaskAtomDetail> getTaskList(long taskSetId, int pageNum, int pageSize) {
        String url = UrlBuilder.ofHttp(BASE_URL)
                .addPath("/api/v3/relay/task_atom")
                .addQuery("set_id", String.valueOf(taskSetId))
                .addQuery("page_num", String.valueOf(pageNum))
                .addQuery("page_size", String.valueOf(pageSize))
                .build();

        HttpRequest httpRequest = HttpRequest.get(url);
        httpRequest = addHeaders(httpRequest);
        String respStr = httpRequest.execute().body();
        GetTaskAtomListResp resp = JSONUtil.toBean(respStr, GetTaskAtomListResp.class);
        BaseRespUtil.checkJobAgentResp(resp);
        return resp.getData().getRecords();
    }

    public Map<String, Boolean> queryNodeJobAgentLiveStatus(List<String> hostList) {
        if (CollectionUtils.isEmpty(hostList)) {
            return Collections.emptyMap();
        }
        JobAgentData jobAgentData = queryNodeJobAgentInfo(hostList);
        List<String> notInstallList = jobAgentData.getNotInstall();

        Map<String, Boolean> resultMap = new HashMap<>();
        hostList.forEach(host -> resultMap.put(host, Boolean.TRUE));

        if (!CollectionUtils.isEmpty(notInstallList)) {
            notInstallList.forEach(host -> resultMap.put(host, Boolean.FALSE));
        }
        return resultMap;
    }

    @Override
    public List<String> queryLostJobAgent(List<String> hostList) {
        if (CollectionUtils.isEmpty(hostList)) {
            return Collections.emptyList();
        }
        JobAgentData jobAgentData = queryNodeJobAgentInfo(hostList);
        return jobAgentData.getNotInstall();
    }

    private JobAgentData queryNodeJobAgentInfo(List<String> hostList) {
        Map<String, List<String>> map = new HashMap<>();
        map.put("hosts", hostList);
        String jsonBody = JSONUtil.toJsonStr(map);
        HttpRequest httpRequest = HttpRequest.post(String.format("%s/%s", BASE_URL, "api/v3/relay/agent_host/compare")).
                header("Content-type", "application/json");
        httpRequest = addHeaders(httpRequest);

        String responseBody = httpRequest.body(jsonBody).execute().body();
        JobAgentCheckResponse jobAgentCheckResponse = JSONUtil.toBean(responseBody, JobAgentCheckResponse.class);
        BaseRespUtil.checkJobAgentResp(jobAgentCheckResponse);
        return jobAgentCheckResponse.getData();
    }


}

