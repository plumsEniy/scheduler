package com.bilibili.cluster.scheduler.api.service.bmr.manager;

import cn.hutool.core.net.url.UrlBuilder;
import cn.hutool.http.HttpRequest;
import com.bilibili.cluster.scheduler.common.Constants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * @description:
 * @Date: 2024/8/13 16:20
 * @Author: nizhiqiang
 */
@Slf4j
@Service
public class BmrManagerServiceImpl implements BmrManagerService {
    @Value("${bmr.base-url:http://uat-cloud-bm.bilibili.co}")
    private String BASE_URL = "http://uat-cloud-bm.bilibili.co";

    @Override
    public void refreshPodInfo(Long clusterId, Long componentId, String clusterName) {
        String url = UrlBuilder.of(BASE_URL)
                .addPath("/api/bmr/cluster/service/deploy/refresh/k8s/pod/list")
                .addQuery("clusterId", String.valueOf(clusterId))
                .addQuery("componentId", String.valueOf(componentId))
                .addQuery("clusterName", clusterName)
                .build();
        log.info("refresh component pod,cluster id is {}, component id is {}, cluster name is ", clusterId, componentId, clusterName);

        String respStr = HttpRequest.get(url)
                .header(Constants.BMR_SYSTEM_INVOKER_KEY, Constants.BMR_SYSTEM_TOKEN)
                .execute().body();

    }
}
