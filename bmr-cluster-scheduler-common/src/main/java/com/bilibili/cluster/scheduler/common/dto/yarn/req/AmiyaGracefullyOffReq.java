package com.bilibili.cluster.scheduler.common.dto.yarn.req;

import cn.hutool.core.annotation.Alias;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AmiyaGracefullyOffReq {

    @Alias(value = "stop_type")
    private String stopType;

    @Alias(value = "stop_wait_time_second")
    private int stopWaitTimeSecond;

    @Alias(value = "stop_wait_log_upload_time_second")
    private int stopWaitLogUploadTimeSecond;

}
