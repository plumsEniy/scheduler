package com.bilibili.cluster.scheduler.common.dto.translation.resp;

import lombok.Data;

import java.util.List;

@Data
public class SqlTranslateResult {

    private boolean translated;

    private String sourceEngineName;

    private String targetEngineName;

    private String sourceEngineSql;

    private String targetEngineSql;

    private String sourceEngineDropSql;

    private String sourceEngineCreateSql;

    private String targetEngineDropSql;

    private String targetEngineCreateSql;

    private String sourceEngineTargetTable;

    private String targetEngineTargetTable;

    private List<String> sourceEngineTargetPartitions;

    private List<String> targetEngineTargetPartitions;

    private String originSqlTargetTable;

    private String targetEngine;

    private String bzTime;

    private boolean hasRandomFunction;



}
