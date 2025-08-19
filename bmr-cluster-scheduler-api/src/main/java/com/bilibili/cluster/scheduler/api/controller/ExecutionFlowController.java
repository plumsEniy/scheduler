package com.bilibili.cluster.scheduler.api.controller;

import cn.hutool.json.JSONUtil;
import com.bilibili.cluster.scheduler.api.enums.RedisLockKey;
import com.bilibili.cluster.scheduler.api.redis.RedissonLockSupport;
import com.bilibili.cluster.scheduler.api.service.GlobalService;
import com.bilibili.cluster.scheduler.api.service.flow.ExecutionFlowService;
import com.bilibili.cluster.scheduler.api.service.flow.ExecutionNodeEventService;
import com.bilibili.cluster.scheduler.api.service.flow.ExecutionNodeService;
import com.bilibili.cluster.scheduler.api.service.flow.check.FlowParamsValidator;
import com.bilibili.cluster.scheduler.api.service.presto.PrestoService;
import com.bilibili.cluster.scheduler.api.tools.Md5Util;
import com.bilibili.cluster.scheduler.api.tools.Result;
import com.bilibili.cluster.scheduler.common.Constants;
import com.bilibili.cluster.scheduler.common.dto.flow.req.DeployOneFlowReq;
import com.bilibili.cluster.scheduler.common.dto.flow.req.QueryFlowListReq;
import com.bilibili.cluster.scheduler.common.dto.flow.req.QueryFlowPageReq;
import com.bilibili.cluster.scheduler.common.dto.flow.req.spark.QuerySparkDeployFlowPageReq;
import com.bilibili.cluster.scheduler.common.dto.node.BatchNodeExecDTO;
import com.bilibili.cluster.scheduler.common.dto.node.req.BatchRetryNodeReq;
import com.bilibili.cluster.scheduler.common.dto.node.req.BatchRollBackNodeReq;
import com.bilibili.cluster.scheduler.common.dto.node.req.BatchSkipNodeReq;
import com.bilibili.cluster.scheduler.common.dto.node.req.QueryNodePageReq;
import com.bilibili.cluster.scheduler.common.enums.flow.FlowOperateButtonEnum;
import com.bilibili.cluster.scheduler.common.response.ResponseResult;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.util.concurrent.TimeUnit;

/**
 * @description: 工作流接口
 * @Date: 2024/1/30 18:57
 */

@RestController
@RequestMapping("/execution")
@Api(tags = "工作流接口")
@Slf4j
public class ExecutionFlowController {

    @Resource
    private ExecutionFlowService executionFlowService;

    @Resource
    private ExecutionNodeService executionNodeService;

    @Resource
    private ExecutionNodeEventService executionNodeEventService;

    @Resource
    RedissonLockSupport redissonLockSupport;

    @Resource
    FlowParamsValidator flowParamsValidator;

    @Resource
    PrestoService prestoService;

    @PostMapping(value = "/query/flow/page", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "工作流分页查询", httpMethod = "POST")
    public ResponseResult queryFlowPage(@RequestBody @Valid QueryFlowPageReq req) {
        return executionFlowService.queryFlowPage(req);
    }

    @PostMapping(value = "/query/node/page", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "任务分页查询", httpMethod = "POST")
    public ResponseResult queryNodePage(@RequestBody @Valid QueryNodePageReq req) {
        return executionNodeService.queryNodePage(req);
    }

    @PostMapping(value = "/query/node/page/download", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "任务下载", httpMethod = "POST")
    public void queryNodePage(@RequestBody @Valid QueryNodePageReq req, HttpServletResponse response) {
        executionNodeService.queryNodePageAndDownLoad(req, response);
    }

    @GetMapping(value = "/query/flow/info", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "查询工作流信息", httpMethod = "GET")
    public ResponseResult queryFlowInfo(@RequestParam Long flowId) {
        return executionFlowService.queryFlowInfo(flowId);
    }


    @GetMapping(value = "/query/flow/runtime/data", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "查询工作流运行时数据", httpMethod = "GET")
    public ResponseResult queryFlowRuntimeData(@RequestParam Long flowId) {
        return executionFlowService.getFlowRuntimeData(flowId);
    }

    @GetMapping(value = "/query/node/event/list", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "查询任务的事件列表", httpMethod = "GET")
    public ResponseResult queryNodeEventList(@RequestParam Long nodeId) {
        return executionNodeEventService.queryNodeEventListByExecutionNodeId(nodeId);
    }

    @GetMapping(value = "/query/node/event/log", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "查询任务事件日志", httpMethod = "GET")
    public ResponseResult queryNodeEventLog(@RequestParam Long nodeEventId) {
        return executionNodeEventService.queryNodeEventLog(nodeEventId);
    }

    @GetMapping(value = "/query/flow/failure/nodes/count", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "查询任务流失败节点数", httpMethod = "GET")
    public Result<Long> queryFlowFailureNodesCount(@RequestParam(value = "flowId") long flowId) {
        long count = executionNodeService.queryFlowFailureNodesCount(flowId);
        return Result.success(count);
    }

    @GetMapping(value = "/alter/flow/status", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "变更工作流操作状态", httpMethod = "GET")
    public ResponseResult alterFlowStatus(@RequestParam(value = "flowId") Long flowId,
                                          @RequestParam(value = "operate") FlowOperateButtonEnum operate) throws Exception {
        return executionFlowService.alterFlowStatus(flowId, operate);
    }

    @PostMapping(value = "/batch/retry/node", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "批量重试任务", httpMethod = "POST")
    public ResponseResult batchRetryNode(@RequestBody @Valid BatchRetryNodeReq req) {
        ResponseResult result =  executionNodeService.batchRetryNode(req);
        return submitNodeExecIfNeed(result, "批量重试任务成功");
    }

    @PostMapping(value = "/batch/retry/execute/failed/event", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "批量重试出错事件并执行", httpMethod = "POST")
    public ResponseResult batchRetryFailedEvent(@RequestBody @Valid BatchRetryNodeReq req) {
        ResponseResult result = executionNodeService.batchRetryFailedEvent(req, false);
        return submitNodeExecIfNeed(result, "批量重试出错事件并执行成功");
    }

    @PostMapping(value = "/batch/skip/execute/failed/event", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "批量跳过出错事件并执行", httpMethod = "POST")
    public ResponseResult batchSkipFailedEvent(@RequestBody @Valid BatchRetryNodeReq req) {
        ResponseResult result =  executionNodeService.batchRetryFailedEvent(req, true);
        return submitNodeExecIfNeed(result, "批量跳过出错事件并执行成功");
    }

    @PostMapping(value = "/batch/skip/node", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "批量跳过任务", httpMethod = "POST")
    public ResponseResult batchSkipNode(@RequestBody @Valid BatchSkipNodeReq req) {
        return executionNodeService.batchSkipNode(req);
    }

    @PostMapping(value = "batch/rollback/node", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "批量回滚任务", httpMethod = "POST")
    public ResponseResult batchRollbackNode(@RequestBody @Valid BatchRollBackNodeReq req) {
        ResponseResult result = executionNodeService.batchRollBackNode(req);
        return submitNodeExecIfNeed(result, "批量回滚任务成功");
    }

    @GetMapping(value = "/flow/execution", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "提交工作流执行", httpMethod = "GET")
    public ResponseResult executionOneFlow(@RequestParam("flowId") Long flowId) {
        boolean isLock = false;
        String lockKey = RedisLockKey.BMR_DEPLOY_SCHEDULER_EXECUTE_ONE_FLOW_LOCK_KEY.name() + flowId;
        try {
            isLock = redissonLockSupport.tryLock(lockKey, Constants.ONE_SECOND * 3, -1, TimeUnit.MILLISECONDS);
            if (!isLock) {
                String errorMsg = String.format("current flow is executing, lock key is %s, flow id:%s", lockKey, flowId);
                log.error(errorMsg);
                return ResponseResult.getError("current flow is starting executing");
            }
            return executionFlowService.executionOneFlow(flowId);
        } finally {
            if (isLock) {
                redissonLockSupport.unLock(lockKey);
            }
        }
    }

    @GetMapping(value = "/flow/cancel", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "取消工作流", httpMethod = "GET")
    public ResponseResult cancelFlow(@RequestParam("flowId") Long flowId) {
        return executionFlowService.cancelFlow(flowId);
    }

    @PostMapping(value = "/apply/one/flow", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "提交一个工作流", httpMethod = "POST")
    public ResponseResult applyFlow(@RequestBody @Valid DeployOneFlowReq req) throws Exception {
        String reqJson = JSONUtil.toJsonStr(req);
        log.info("trace_id: {}, apply scheduler deploy req : {}.", MDC.get(Constants.LOG_TRACE_ID), reqJson);
        String lock = RedisLockKey.BMR_DEPLOY_SCHEDULER_APPLY_ONE_FLOW_LOCK_KEY.name() + Md5Util.getMD5Hash(reqJson);
        Boolean isLock = redissonLockSupport.tryLock(lock, Constants.ONE_SECOND * 3, -1, TimeUnit.MILLISECONDS);
        if (!isLock) {
            throw new IllegalArgumentException(String.format("获取lock失败%s, 请稍后...", lock));
        }
        try {
            flowParamsValidator.validate(req);
            log.info("validate apply req ok");
            return executionFlowService.applyFlow(req);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw e;
        } finally {
            if (isLock) {
                redissonLockSupport.unLock(lock);
            }
        }
    }

    @GetMapping(value = "/query/flow/pipeline/define/info", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "查询工作流pipeline定义", httpMethod = "GET")
    public ResponseResult queryPipelineDefine(@RequestParam("flowId") Long flowId) throws Exception {
        return executionFlowService.queryPipelineDefine(flowId);
    }

    @PostMapping(value = "/query/spark/deploy/flow/page/list", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "查询spark版本发布列表", httpMethod = "POST")
    public ResponseResult querySparkDeployFlowList(@RequestBody @Valid QuerySparkDeployFlowPageReq req) {
        return executionFlowService.querySparkDeployFlowPageList(req);
    }

    @GetMapping(value = "/query/spark/jobId/deploy/flow/list", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "查询spark发布记录中某一任务任务的变更历史", httpMethod = "GET")
    public ResponseResult querySparkDeployFlowListByJobId(@RequestParam("jobId") String jobId) {
        return executionFlowService.querySparkDeployFlowListByJobId(jobId);
    }

    @PostMapping(value = "/query/flow/list", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "批量查询发布列表", httpMethod = "POST")
    public ResponseResult querySparkDeployFlowList(@RequestBody @Valid QueryFlowListReq req) {
        return executionFlowService.queryFlowInfoList(req);
    }

    @GetMapping(value = "/query/spark/client/deploy/history/list", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "查询某一主机的spark客户端发布历史", httpMethod = "GET")
    public ResponseResult querySparkDeployFlowList(@RequestParam("nodeName") String nodeName) {
        return executionFlowService.querySparkClientDeployInfoByNodeName(nodeName);
    }

    @GetMapping(value = "/delete/flow/info/by/id", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "删除指定发布单信息", httpMethod = "GET")
    public ResponseResult querySparkDeployFlowList(@RequestParam("flowId") Long flowId) {
        return executionFlowService.deleteFlowById(flowId);
    }

    @GetMapping(value = "/admin/modify/flow/status", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "admin修改审批状态", httpMethod = "GET")
    public ResponseResult adminModifyFlowStatus(@RequestParam("flowId") Long flowId,
                                                   @RequestParam("flowState") String flowState) {
        return executionFlowService.adminModifyFlowStatus(flowId, flowState);
    }


    @GetMapping(value = "/op/cancel/presto/caster/node/taint/status", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "运维场景-取消presto节点污点配置", httpMethod = "GET")
    public ResponseResult cancelNodeTaintStatus(@RequestParam("hostname") String hostname) {
        return prestoService.cancelNodeTaintStatus(hostname);
    }

    @GetMapping(value = "/op/add/presto/caster/node/taint/status", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "运维场景-增加presto节点污点配置", httpMethod = "GET")
    public ResponseResult addNodeTaintStatus(@RequestParam("hostname") String hostname,
                                                @RequestParam("appId") String appId) {
        return prestoService.addNodeTaintStatus(hostname, appId);
    }

    private ResponseResult submitNodeExecIfNeed(ResponseResult result, String sucMsg) {
        if (result.getCode() == 0) {
            try {
                BatchNodeExecDTO batchNodeExecDTO = (BatchNodeExecDTO) result.getObj();
                executionNodeService.handlerJobTaskEvent(batchNodeExecDTO.getTargetExecutionNodeList(), batchNodeExecDTO.getFlowEntity());
                return ResponseResult.getSuccess(sucMsg);
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                return ResponseResult.getError(e.getMessage());
            }
        } else {
            return result;
        }
    }

}
