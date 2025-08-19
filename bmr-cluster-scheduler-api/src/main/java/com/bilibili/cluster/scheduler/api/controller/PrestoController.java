package com.bilibili.cluster.scheduler.api.controller;

import com.bilibili.cluster.scheduler.api.service.presto.PrestoService;
import com.bilibili.cluster.scheduler.common.response.ResponseResult;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * @description: presto控制器
 * @Date: 2024/6/7 14:23
 * @Author: nizhiqiang
 */

@RestController
@RequestMapping("/presto")
@Api(tags = "presto的控制器")
@Slf4j
public class PrestoController {


    @Resource
    PrestoService prestoService;

    @GetMapping(value = "query/template", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "查询模版", httpMethod = "GET")
    public ResponseResult queryPrestoTemplate(@RequestParam long clusterId,
                                              @RequestParam long configVersionId,
                                              @RequestParam String image) {
        return ResponseResult.getSuccess(prestoService.queryPrestoTemplate(clusterId, configVersionId, image));
    }
}
