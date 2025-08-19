package com.bilibili.cluster.scheduler.api.controller;

import com.bilibili.cluster.scheduler.api.service.experiment.ExperimentJobService;
import com.bilibili.cluster.scheduler.common.dto.bmr.experiment.ExperimentJobResultDTO;
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

@Slf4j
@RestController
@RequestMapping("/spark")
@Api(tags = "与spark-manager交互的控制器")
public class SparkManagerController {

    @Resource
    ExperimentJobService experimentJobService;

    // fast path for experiment task query
    @GetMapping(value = "/ci/query/node/experiment/result", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "查询单个spark实验任务的执行结果", httpMethod = "GET")
    public ResponseResult querySparkDeployFlowList(@RequestParam("nodeId") Long nodeId) {
        ExperimentJobResultDTO jobResultDTO = experimentJobService.queryExperimentJobResultByExecNodeId(nodeId);
        return ResponseResult.getSuccess(jobResultDTO);
    }

}
