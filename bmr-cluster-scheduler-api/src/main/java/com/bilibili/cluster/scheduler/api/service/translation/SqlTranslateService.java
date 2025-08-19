package com.bilibili.cluster.scheduler.api.service.translation;

import com.bilibili.cluster.scheduler.common.dto.translation.req.SqlTranslateReq;
import com.bilibili.cluster.scheduler.common.dto.translation.resp.SqlTranslateResult;

public interface SqlTranslateService {

    SqlTranslateResult getSqlTranslateResult(SqlTranslateReq sqlTranslateReq);

}
