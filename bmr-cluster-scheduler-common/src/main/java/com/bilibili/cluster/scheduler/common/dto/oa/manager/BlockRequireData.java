package com.bilibili.cluster.scheduler.common.dto.oa.manager;

import cn.hutool.core.annotation.Alias;
import lombok.Data;

@Data
public class BlockRequireData {

    @Alias(value= "platform_id")
    private long platformId;

    @Alias(value= "c_uuid")
    private String cuuid;

    @Alias(value= "exec_time_unix")
    private long execTimeUnix;

    @Alias(value= "env")
    private int env;

    /**
     * 操作人ad_account
     */
    private String operator;

    @Alias(value= "source_type")
    private int sourceType;

    @Alias(value= "specific_source")
    private String specificSource;

    @Alias(value= "behavior_id")
    private String behaviorId;

    @Alias(value= "replace_role")
    private ReplaceRoleModel replaceRole;

}
