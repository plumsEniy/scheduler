package com.bilibili.cluster.scheduler.common.dto.oa.manager;

import lombok.Data;

@Data
public class OaChangeInfo {

    private String changeComponent;

    private String env;

    private String changeType;

    private String remark;

}
