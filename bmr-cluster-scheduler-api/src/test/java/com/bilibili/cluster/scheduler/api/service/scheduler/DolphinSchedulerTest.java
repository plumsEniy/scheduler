package com.bilibili.cluster.scheduler.api.service.scheduler;

import cn.hutool.core.net.url.UrlBuilder;
import cn.hutool.http.Header;
import cn.hutool.http.HttpRequest;
import cn.hutool.json.JSONUtil;
import com.bilibili.cluster.scheduler.common.dto.scheduler.resp.PipelineDefinitionResp;
import com.bilibili.cluster.scheduler.common.dto.scheduler.resp.ProjectResp;

public class DolphinSchedulerTest {
    public static void main(String[] args) {
//        String url = UrlBuilder.ofHttp("http://bmr.scheduler.bilibili.co/dolphinscheduler").addPath(String.format("/projects/list")).build();
//        String result = HttpRequest.get(url)
//                .header(Header.CONTENT_TYPE, "application/json")
//                .header("token", "69f6064be4eebff175ad602e60ef3fba")
//                .timeout(20000)// 超时，毫秒
//                .execute().body();
//        System.out.println(result);
//
//        ProjectResp bean = JSONUtil.toBean(result, ProjectResp.class);
//        System.out.println(bean.getData().get(0).getName());


        String url = UrlBuilder.ofHttp("http://bmr.scheduler.bilibili.co/dolphinscheduler").addPath(String.format("projects/%s/process-definition/query-by-name", 14527961473760L)).build();
        String result = HttpRequest.get(url)
                .header(Header.CONTENT_TYPE, "application/json")
                .header("token", "69f6064be4eebff175ad602e60ef3fba")
                .form("name","Amiya")
                .timeout(20000)// 超时，毫秒
                .execute().body();
        System.out.println(result);

        PipelineDefinitionResp resp = JSONUtil.toBean(result, PipelineDefinitionResp.class);
        String projectCode = resp.getData().getProcessDefinition().getProjectCode();
        System.out.println(projectCode);
    }
}
