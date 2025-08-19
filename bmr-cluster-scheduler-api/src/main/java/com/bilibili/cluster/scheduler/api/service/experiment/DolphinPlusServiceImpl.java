package com.bilibili.cluster.scheduler.api.service.experiment;

import cn.hutool.core.net.url.UrlBuilder;
import cn.hutool.http.HttpRequest;
import cn.hutool.json.JSONUtil;
import com.bilibili.cluster.scheduler.common.Constants;
import com.bilibili.cluster.scheduler.common.dto.spark.plus.ExperimentJobResult;
import com.bilibili.cluster.scheduler.common.dto.spark.plus.req.CreateExperimentRequest;
import com.bilibili.cluster.scheduler.common.dto.spark.plus.req.QueryExperimentRequest;
import com.bilibili.cluster.scheduler.common.dto.spark.plus.CreateExperimentData;
import com.bilibili.cluster.scheduler.common.dto.spark.plus.resp.BaseDolphinPlusResp;
import com.bilibili.cluster.scheduler.common.dto.spark.plus.resp.CreateExperimentResponse;
import com.bilibili.cluster.scheduler.common.dto.spark.plus.resp.QueryExperimentResponse;
import com.bilibili.cluster.scheduler.common.exception.DolphinPlusInvokerException;
import com.google.common.base.Preconditions;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

@Slf4j
@Component
public class DolphinPlusServiceImpl implements DolphinPlusService {

    @Value("${spark.experiment.plus.host}")
    String sqlTranslateHost;

    @Override
    public CreateExperimentData createExperimentTask(CreateExperimentRequest request) {
        final String url = UrlBuilder.ofHttp(sqlTranslateHost)
                .addPath("/experiment/create").build();
        String body = JSONUtil.toJsonStr(request);
        log.info("request to createExperimentTask, url is {}, body is {}", url, body);

        String respJson = HttpRequest.post(url).body(body)
                .header(Constants.CONTENT_TYPE, Constants.APPLICATION_JSON)
                .execute().body();
        log.info("request to createExperimentTask resp is {}", respJson);

        CreateExperimentResponse response = JSONUtil.toBean(respJson, CreateExperimentResponse.class);
        checkBaseResp(response);
        List<CreateExperimentData> data = response.getData();
        Preconditions.checkState(!CollectionUtils.isEmpty(data), "data list is empty.");

        return data.get(0);
    }

    @Override
    public ExperimentJobResult queryExperimentResult(QueryExperimentRequest request) {
        final String url = UrlBuilder.ofHttp(sqlTranslateHost)
                .addPath("/dolphinplus/getJobExecutionInfo").build();
        String body = JSONUtil.toJsonStr(request);
        log.info("request to getJobExecutionInfo, url is {}, body is {}", url, body);
        String respJson = HttpRequest.post(url).body(body)
                .header(Constants.CONTENT_TYPE, Constants.APPLICATION_JSON)
                .execute().body();
        log.info("request to getJobExecutionInfo resp is {}", respJson);

        QueryExperimentResponse response = JSONUtil.toBean(respJson, QueryExperimentResponse.class);
        checkBaseResp(response);
        List<List<ExperimentJobResult>> data = response.getData();
        Preconditions.checkState(!CollectionUtils.isEmpty(data), "data list is empty.");
        List<ExperimentJobResult> experimentDataList = data.get(0);
        Preconditions.checkState(!CollectionUtils.isEmpty(experimentDataList), "data list is empty.");
        return experimentDataList.get(0);
    }

    private void checkBaseResp(BaseDolphinPlusResp baseResp) {
        if (Objects.isNull(baseResp)) {
            throw new DolphinPlusInvokerException("response is null");
        }
        if (baseResp.getErrorCode() != 0) {
            throw new DolphinPlusInvokerException(baseResp.getStackTrace());
        }
    }

}
