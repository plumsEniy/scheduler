package com.bilibili.cluster.scheduler.common.dto.translation.req;

import cn.hutool.core.annotation.Alias;
import lombok.Data;

@Data
public class SqlTranslateReq {

    private String query;

    @Alias(value = "confs")
    private SqlTranslateConf conf = new SqlTranslateConf();

}
