package com.bilibili.cluster.scheduler.api.service.translation;

import cn.hutool.core.net.url.UrlBuilder;
import cn.hutool.http.HttpRequest;
import cn.hutool.json.JSONUtil;
import com.bilibili.cluster.scheduler.common.Constants;
import com.bilibili.cluster.scheduler.common.dto.translation.req.SqlTranslateReq;
import com.bilibili.cluster.scheduler.common.dto.translation.resp.SqlTranslateResp;
import com.bilibili.cluster.scheduler.common.dto.translation.resp.SqlTranslateResult;
import com.google.common.base.Preconditions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SqlTranslateServiceImpl implements SqlTranslateService {

    @Value("${spark.sql.translate.host}")
    String sqlTranslateHost;

    @Override
    public SqlTranslateResult getSqlTranslateResult(SqlTranslateReq sqlTranslateReq) {
        final String url = UrlBuilder.ofHttp(sqlTranslateHost)
                .addPath("/api/translations/translateAndReplace").build();
        String body = JSONUtil.toJsonStr(sqlTranslateReq);
        log.info("request to getSqlTranslateResult, url is {}, body is {}", url, body);

        String respJson = HttpRequest.post(url).body(body)
                .header(Constants.CONTENT_TYPE, Constants.APPLICATION_JSON)
                .execute().body();

        log.info("request to getSqlTranslateResult resp is {}", respJson);
        SqlTranslateResp sqlTranslateResp = JSONUtil.toBean(respJson, SqlTranslateResp.class);

        Preconditions.checkNotNull(sqlTranslateResp, "sqlTranslateResp is null");
        Preconditions.checkNotNull(sqlTranslateResp.getData(), "sqlTranslateResp.data is null");

        return sqlTranslateResp.getData().get(0);
    }

}
