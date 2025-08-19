package com.bilibili.cluster.scheduler.common.dto.oa.manager;

import cn.hutool.core.annotation.Alias;
import lombok.Data;

@Data
public class ReplaceRoleModel {

    @Alias(value = "dev_leader")
    private String[] devLeader;

    @Alias(value = "sre_leader")
    private String[] sreLeader;

}
