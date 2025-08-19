package com.bilibili.cluster.scheduler.common.dto.translation.resp;

import lombok.Data;

import java.util.List;

@Data
public class SqlTranslateResp {

    private List<SqlTranslateResult> data;

}
