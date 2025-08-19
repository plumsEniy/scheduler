package com.bilibili.cluster.scheduler.common.dto.dns;

import cn.hutool.core.annotation.Alias;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @description:
 * @Date: 2025/4/22 11:33
 * @Author: nizhiqiang
 */
@NoArgsConstructor
@Data
public class DnsInfo {

    private Integer id;
    private String description;
    @Alias("domain_id")
    private Integer domainId;
    @Alias("domain_name")
    private String domainName;
    @Alias("record_name")
    private String recordName;
    @Alias("record_type")
    private String recordType;
    @Alias("record_value")
    private String recordValue;
    @Alias("record_line_id")
    private Integer recordLineId;

    @Alias("record_line_name")
    private String recordLineName;

    private String ttl;

    private String updateAt;

    private String operator;
}
