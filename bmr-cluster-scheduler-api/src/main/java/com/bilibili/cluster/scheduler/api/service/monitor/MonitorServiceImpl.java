package com.bilibili.cluster.scheduler.api.service.monitor;

import cn.hutool.core.net.url.UrlBuilder;
import cn.hutool.http.HttpRequest;
import cn.hutool.json.JSONUtil;
import com.bilibili.cluster.scheduler.common.dto.monitor.dto.MonitorInfo;
import com.bilibili.cluster.scheduler.common.dto.monitor.dto.MonitorResult;
import com.bilibili.cluster.scheduler.common.dto.monitor.dto.MonitorValue;
import com.bilibili.cluster.scheduler.common.dto.monitor.resp.QueryMonitorResp;
import com.bilibili.cluster.scheduler.common.utils.BaseRespUtil;
import com.bilibili.cluster.scheduler.common.utils.NumberUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @description: 监控指标
 * @Date: 2025/4/27 17:22
 * @Author: nizhiqiang
 */
@Slf4j
@Component
public class MonitorServiceImpl implements MonitorService {

    private static String BASE_URL = "http://hawkeye.bilibili.co";

    private static String TOKEN = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJleHAiOjQ4NDA5MzU0MzEsImlhdCI6MTY4NzMzNTQzMSwiaXNzIjoibGl1bWluZ2dhbmciLCJVc2VybmFtZSI6ImxpdW1pbmdnYW5nIiwiRXhwaXJlQXQiOiIwMDAxLTAxLTAxVDAwOjAwOjAwWiJ9.jRvsUI6bs2qXVMr7Xa1W2HcFZdHlgK5bZjVO4QwZrhM";

    private static int DATA_SOURCE_ID = 81;

    @Override
    public MonitorValue queryMonitor(String promsql, Long timeStamp) {
        UrlBuilder urlBuilder = UrlBuilder.ofHttp(BASE_URL)
                .addPath("/report/api/v2/query/datasource")
                .addPath(String.valueOf(DATA_SOURCE_ID));

        if (NumberUtils.isPositiveLong(timeStamp)) {
            urlBuilder.addQuery("time", String.valueOf(timeStamp));
        }
        urlBuilder.addQuery("query", promsql);

        String url = urlBuilder.build();
        log.info("queryMonitor url:{}", url);

        String respStr = HttpRequest.get(url)
                .header("Authorization", TOKEN)
                .execute()
                .body();
        log.info("queryMonitor respStr:{}", respStr);
        QueryMonitorResp resp = JSONUtil.toBean(respStr, QueryMonitorResp.class);
        BaseRespUtil.checkCommonResp(resp);
        return Optional.of(resp.getData())
                .map(MonitorInfo::getResult)
                .map(resultList -> resultList.get(0))
                .map(MonitorResult::getValue)
                .map(MonitorValue::generateMonitorValue)
                .get();

    }

    @Override
    public List<MonitorValue> queryRangeMonitor(String promsql, Long start, Long end, Double step) {
        String url = UrlBuilder.ofHttp(BASE_URL)
                .addPath("/report/api/v2/query_range/datasource")
                .addPath(String.valueOf(DATA_SOURCE_ID))
                .addQuery("start", String.valueOf(start))
                .addQuery("end", String.valueOf(end))
                .addQuery("step", String.valueOf(step))
                .addQuery("query", promsql)
                .build();

        String respStr = HttpRequest.get(url)
                .header("Authorization", TOKEN)
                .execute()
                .body();
        log.info("queryMonitor respStr:{}", respStr);
        QueryMonitorResp resp = JSONUtil.toBean(respStr, QueryMonitorResp.class);
        BaseRespUtil.checkCommonResp(resp);
        return Optional.of(resp.getData())
                .map(MonitorInfo::getResult)
                .map(resultList -> resultList.get(0))
                .map(MonitorResult::getValues)
                .map(valueList -> {
                    return valueList.stream()
                            .map(MonitorValue::generateMonitorValue)
                            .collect(Collectors.toList());
                })
                .get();
    }
}
