package com.bilibili.cluster.scheduler.common.dto.hbo.model;

import lombok.Data;

/**
 * @description:
 * @Date: 2024/12/27 11:19
 * @Author: nizhiqiang
 */

@Data
public class HboJob {
    private String jobId;

    private String paramList;

    private Boolean isDeleted = false;

}
