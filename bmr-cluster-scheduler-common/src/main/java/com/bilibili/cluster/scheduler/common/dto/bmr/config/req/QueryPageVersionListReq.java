package com.bilibili.cluster.scheduler.common.dto.bmr.config.req;

import lombok.Data;

@Data
public class QueryPageVersionListReq {

    private int pageNum;

    private int pageSize;

    private Long componentId;

    private String versionName;

}