package com.bilibili.cluster.scheduler.api.controller;

import com.bilibili.cluster.scheduler.api.service.clickhouse.clickhouse.ClickhouseService;
import com.bilibili.cluster.scheduler.common.dto.clickhouse.clickhouse.req.QueryCapacityTemplateReq;
import com.bilibili.cluster.scheduler.common.dto.clickhouse.clickhouse.req.QueryIterationTemplateReq;
import com.bilibili.cluster.scheduler.common.response.ResponseResult;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * @description:
 * @Date: 2025/2/8 14:23
 * @Author: nizhiqiang
 */

@RestController
@RequestMapping("/clickhouse")
@Api(tags = "clickhouse的控制器")
@Slf4j
public class ClickHouseController {

    @Resource
    ClickhouseService clickhouseService;

    @GetMapping(value = "query/template", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "查询caster模版", httpMethod = "GET")
    public ResponseResult queryClickhouseTemplate(@RequestParam long configVersionId) {
        return ResponseResult.getSuccess(clickhouseService.buildClickhouseDeployDTO(configVersionId));
    }

    @GetMapping(value = "query/pod/template/list", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "查询pod模版列表", httpMethod = "GET")
    public ResponseResult queryPodTemplateList(@RequestParam long configVersionId) {
        return ResponseResult.getSuccess(clickhouseService.queryPodTemplateList(configVersionId));
    }

    @GetMapping(value = "query/pod/template/list/by/cluster/id", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "根据集群id查询pod模版列表", httpMethod = "GET")
    public ResponseResult queryPodTemplateListByClusterId(@RequestParam long clusterId) {
        return ResponseResult.getSuccess(clickhouseService.queryPodTemplateListByClusterId(clusterId));
    }

    @PostMapping(value = "query/capacity/template", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "查询clickhouse扩容模版", httpMethod = "POST")
    public ResponseResult queryClickhouseCapacityTemplate(@RequestBody QueryCapacityTemplateReq req) {
        return ResponseResult.getSuccess(clickhouseService.buildScaleDeployDTO(req.getConfigVersionId(), req.getPodTemplate(), req.getShardAllocationList()));
    }

    @PostMapping(value = "query/iteration/template", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "查询clickhouse迭代模版", httpMethod = "POST")
    public ResponseResult queryClickhouseIterationTemplate(@RequestBody QueryIterationTemplateReq req) {
        return ResponseResult.getSuccess(clickhouseService.buildIterationDeployDTO(req.getConfigId(), req.getPodTemplate(), req.getNodeList()));
    }


    @GetMapping(value = "query/shard/list", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "查询当前shard分批列表", httpMethod = "GET")
    public ResponseResult queryShardList(@RequestParam long configVersionId) {
        return ResponseResult.getSuccess(clickhouseService.queryShardList(configVersionId));
    }

    @GetMapping(value = "get/pod/url", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "获取pod的链接", httpMethod = "GET")
    public ResponseResult queryPodUrl(@RequestParam long clusterId, @RequestParam long configVersionId, @RequestParam String podName) {
        return ResponseResult.getSuccess(clickhouseService.getPodUrl(clusterId, configVersionId, podName));
    }

    @GetMapping(value = "query/pod/log", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "获取pod的日志", httpMethod = "GET")
    public ResponseResult queryPodLog(@RequestParam long clusterId, @RequestParam long configVersionId, @RequestParam String podName, @RequestParam int limit) {
        return ResponseResult.getSuccess(clickhouseService.queryPodLog(clusterId, configVersionId, podName, limit));
    }
}
