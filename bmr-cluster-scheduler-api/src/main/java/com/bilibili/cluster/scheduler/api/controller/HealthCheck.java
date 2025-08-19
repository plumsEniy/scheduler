package com.bilibili.cluster.scheduler.api.controller;

import com.bilibili.cluster.scheduler.common.response.ResponseResult;
import io.swagger.annotations.Api;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Api(tags = "健康检查", produces = MediaType.APPLICATION_JSON_VALUE)
public class HealthCheck {
    /**
     * Check network result.
     *
     * @return the result
     */
    @GetMapping(value = "/monitor/ping", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseResult checkNetwork() {
        return ResponseResult.getSuccess("OK");
    }
}
