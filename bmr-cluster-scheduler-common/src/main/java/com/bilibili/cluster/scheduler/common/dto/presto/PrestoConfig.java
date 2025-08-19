package com.bilibili.cluster.scheduler.common.dto.presto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @description: presto的配置
 * @Date: 2024/6/4 16:53
 * @Author: nizhiqiang
 */

@Data
public class PrestoConfig {

    private String memoryLimit;
    private String cpuLimit;
    private Integer count;
    private Boolean localDiskEnabled;
    private Map<String,String> additionalPropMap;
    private List<String> additionalJVMConfigList;

}
