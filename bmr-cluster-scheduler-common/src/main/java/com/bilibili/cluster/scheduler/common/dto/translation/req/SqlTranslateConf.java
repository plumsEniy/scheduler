package com.bilibili.cluster.scheduler.common.dto.translation.req;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SqlTranslateConf {

    private String sourceLanguage = "spark";
    private String targetLanguage = "spark";
    private String sourceEngineName = "spark 3.1";
    private String targetEngineName = "spark 4.0";

    public static SqlTranslateConf getTrinoConf() {
        return new SqlTranslateConf("trino", "trino",
                "", "");
    }

}
