package com.bilibili.cluster.scheduler.api.service.flow;

import com.baomidou.mybatisplus.extension.service.IService;
import com.bilibili.cluster.scheduler.api.exceptions.WorkflowInstanceTaskEventHandleException;
import com.bilibili.cluster.scheduler.common.dto.node.dto.ExecutionNodeSummary;
import com.bilibili.cluster.scheduler.common.dto.node.req.BatchRetryNodeReq;
import com.bilibili.cluster.scheduler.common.dto.node.req.BatchRollBackNodeReq;
import com.bilibili.cluster.scheduler.common.dto.node.req.BatchSkipNodeReq;
import com.bilibili.cluster.scheduler.common.dto.node.req.QueryNodePageReq;
import com.bilibili.cluster.scheduler.common.entity.ExecutionFlowEntity;
import com.bilibili.cluster.scheduler.common.entity.ExecutionNodeEntity;
import com.bilibili.cluster.scheduler.common.enums.NodeExecuteStatusEnum;
import com.bilibili.cluster.scheduler.common.enums.event.EventStatusEnum;
import com.bilibili.cluster.scheduler.common.enums.node.NodeExecType;
import com.bilibili.cluster.scheduler.common.enums.node.NodeOperationResult;
import com.bilibili.cluster.scheduler.common.response.ResponseResult;

import javax.servlet.http.HttpServletResponse;
import java.time.LocalDateTime;
import java.util.List;

/**
 * <p>
 * 服务类
 * </p>
 *
 * @author 谢谢谅解
 * @since 2024-01-26
 */
public interface ExecutionNodeService extends IService<ExecutionNodeEntity> {

    /**
     * 根据工作流和批次查询节点
     *
     * @param flowId
     * @param batchId
     * @return
     */
    public List<ExecutionNodeEntity> queryExecutionNodeByBatchIdAndFlowId(Long flowId, Integer batchId);

    /**
     * 根据flowId 和状态查询
     */
    public List<ExecutionNodeEntity> queryByFlowIdAndBatchIdAndNodeStatus(Long flowId, Integer batchId, List<NodeExecuteStatusEnum> jobStatus);

    /**
     * 根据job状态查询
     */
    public List<ExecutionNodeEntity> findExecuteNodeByNodeStatus(List<NodeExecuteStatusEnum> nodeStatus);

    /**
     * 修改job状态
     */
    public void updateNodeStatusById(Long id, NodeExecuteStatusEnum nodeExecuteStatusEnum);

    /**
     * 批量修改任务执行状态
     *
     * @param jobIdList
     * @param nodeExecuteStatusEnum
     */
    void batchUpdateNodeStatus(List<Long> jobIdList, NodeExecuteStatusEnum nodeExecuteStatusEnum);

    /**
     * 修改job状态
     */
    public void updateNodeStatusByFlowIdAndNodeStatus(Long flowId, NodeExecuteStatusEnum originJobExecuteStatus, NodeExecuteStatusEnum targetJobExecuteStatus);

    /**
     * 修改job状态
     */
    public void updateNodeStatusByFlowIdAndBatchId(Long flowId, Integer batchId, NodeExecuteStatusEnum nodeExecuteStatusEnum);


    /**
     * 批量插入
     *
     * @param executionJobEntityList
     * @return
     */
    boolean batchInsert(List<ExecutionNodeEntity> executionJobEntityList);

    /**
     * 分页查询job
     *
     * @param req
     * @return
     */
    ResponseResult queryNodePage(QueryNodePageReq req);

    /**
     * 根据flowid查询job
     *
     * @param flowId
     * @return
     */
    List<ExecutionNodeEntity> queryAllExecutionNodeByFlowId(Long flowId);

    /**
     * 更新任务执行开始时间, 或者结束时间
     */
    void updateNodeStartTimeOrEndTime(Long id, long instanceId, LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 批量重试任务
     *
     * @param req
     * @return
     */
    ResponseResult batchRetryNode(BatchRetryNodeReq req);

    /**
     * 批量回滚任务
     *
     * @param req
     * @return
     */
    ResponseResult batchRollBackNode(BatchRollBackNodeReq req);

    /**
     * recovery场景, 恢复执行中的job
     */
    void recoveryFlowNode(ExecutionFlowEntity executionFlowEntity) throws WorkflowInstanceTaskEventHandleException;

    /**
     * 批量更新job操作结果
     *
     * @param jobIdList
     * @param operationResult
     */
    void batchUpdateNodeOperationResult(List<Long> jobIdList, NodeOperationResult operationResult);

    /**
     * 更新job操作结果
     *
     * @param jobId
     * @param operationResult
     */
    void updateNodeOperationResultByNodeId(Long jobId, NodeOperationResult operationResult, long instanceId);

    /**
     * 批量跳过任务
     *
     * @param req
     * @return
     */
    ResponseResult batchSkipNode(BatchSkipNodeReq req);

    void queryNodePageAndDownLoad(QueryNodePageReq req, HttpServletResponse response);

    /**
     * 根据工作流id查询节点
     *
     * @param flowId
     * @return
     */
    List<ExecutionNodeEntity> queryExecutionNodeByFlowId(Long flowId);

    /**
     * 批量更新Node节点执行实例Id
     * @param flowId
     * @param instanceId
     * @param nodeIdList
     */
    void updateNodeInstanceId(Long flowId, Long instanceId, List<Long> nodeIdList, String execHost);

    /**
     * 查询指定执行实例id对应的节点列表
     * @param flowId
     * @param instanceId
     * @return
     */
    List<ExecutionNodeEntity> queryNodeListByInstanceId(long flowId, long instanceId);

    /**
     * 根据主机名查询发布节点信息
     * @param flowId
     * @param hostname
     * @return
     */
    List<ExecutionNodeEntity> queryByHostname(Long flowId, String hostname);

    /**
     * 根据主机名查询发布节点信息
     * @param flowId
     * @param hostname
     * @return
     */
    ExecutionNodeEntity queryByHostnameAndInstanceId(Long flowId, String hostname, Long instanceId);

    /**
     *
     * @param collect
     * @param unNodeRetryExecute
     */
    void batchUpdateNodeStatusAndResetInstanceId(List<Long> collect, NodeExecuteStatusEnum unNodeRetryExecute);

    /**
     * 更新节点执行状态 by instanceId
     * @param executionNodeId
     * @param instanceId
     * @param failNodeExecute
     */
    void updateNodeStatusByIdAndInstanceId(Long executionNodeId, long instanceId, NodeExecuteStatusEnum failNodeExecute);

    /**
     * 根据状态获取对齐的节点列表
     * @param flowId
     * @param instanceId
     * @param eventOrder
     * @param eventStatusList
     * @return
     */
    List<ExecutionNodeEntity> getAlignNodeListByEventStatus(Long flowId, Long instanceId, int eventOrder, List<EventStatusEnum> eventStatusList);

    /**
     * 根据状态获取对齐的节点Id列表
     * @param flowId
     * @param instanceId
     * @param eventOrder
     * @param eventStatusList
     * @return
     */
    List<Long> getAlignNodeIdListByEventStatus(Long flowId, Long instanceId, int eventOrder, List<EventStatusEnum> eventStatusList);

    /**
     * query one node by condition
     * @return
     */
    ExecutionNodeEntity queryOneNode(ExecutionNodeEntity queryDo);

    /**
     * query node list by condition
     * @return
     */
    List<ExecutionNodeEntity> queryNodeList(ExecutionNodeEntity queryDo, boolean isAsc);

    /**
     * 更新执行节点和节点event列表的instanceId，需要同时更新
     * @param flowId
     * @param instanceId
     * @param nodeIdList
     */
    void updateNodeAndEventInstanceId(Long flowId, Long instanceId, List<Long> nodeIdList, String execHost);

    /**
     * 快速执行入口
     * @param executionJobEntityList
     * @param executionFlowEntity
     * @throws WorkflowInstanceTaskEventHandleException
     */
    void handlerJobTaskEvent(List<ExecutionNodeEntity> executionJobEntityList, ExecutionFlowEntity executionFlowEntity) throws WorkflowInstanceTaskEventHandleException;


    /**
     * 查询最大批次id
     *
     * @param flowId
     */
    Integer queryMaxBatchId(Long flowId);

    /**
     * 查询失败节点数量
     * @param flowId
     * @return
     */
    long queryFlowFailureNodesCount(long flowId);

    /**
     * 根据pod名清理pod状态
     * @param flowId
     * @param podNameList
     */
    void clearPodByPodName(Long flowId, List<String> podNameList);

    /**
     * 更新pod名
     * @param nodeId
     * @param podName
     */
    void updatePodName(Long nodeId, String podName);

    /**
     * 更新pod状态
     * @param nodeId
     * @param podStatus
     */
    void updatePodStatus(Long nodeId, String podStatus);

    /**
     * 查询节点执行数量，group by stage and status
     * @param flowId
     * @return
     */
    List<ExecutionNodeSummary> queryExecutionNodeSummary(Long flowId);

    /**
     * 查询当前执行阶段信息
     * @param batchId
     * @param flowId
     * @return
     */
    String queryCurStage(long flowId, Integer batchId);

    /**
     * 查询阶段最大节点Id
     * @param flowId
     * @param execStage
     * @return
     */
    Long queryMaxNodeIdByStage(Long flowId, String execStage);


    /**
     * 查询工作流最大stage
     * @param flowId
     * @return
     */
    String queryMaxStageByFlowId(Long flowId);

    /**
     * 查询工作流最小stage
     * @param flowId
     * @return
     */
    String queryMinStageByFlowId(Long flowId);

    /**
     * 更新节点状态至可执行状态
     * @param nodeIds
     * @return
     */
    boolean batchUpdateNodeForReadyExec(Long flowId, List<Long> nodeIds, NodeExecType execType, NodeExecuteStatusEnum nodeExecuteStatus);

    /**
     * 查询当前阶段最下batchId
     * @param flowId
     * @param curStage
     * @return
     */
    int queryMinBatchIdByStage(Long flowId, String curStage);


    /**
     * 查询当前批次执行节点（任一）
     * @param flowId
     * @param batchId
     * @return
     */
    ExecutionNodeEntity queryCurExecOneNode(long flowId, Integer batchId);

    /**
     * 批量更新节点状态和执行实例Id
     * @param nodeIdList
     * @param nextNodeStatus
     * @param instanceId
     * @param execHost
     */
    void batchUpdateNodeStatusAndInstanceId(long flowId, List<Long> nodeIdList,
                                            NodeExecuteStatusEnum nextNodeStatus,
                                            Long instanceId, String execHost);

    /**
     * 批量重试错误节点，从错误事件开始执行，需要判断该事件可重试，当前dolphin-scheduler非初始节点不支持重试
     * @param req
     * @return
     */
    ResponseResult batchRetryFailedEvent(BatchRetryNodeReq req, boolean skipFailedEvent);

    /**
     * 查询当前flow运行中的任务数量
     * @param flowId
     * @return
     */
    int queryCurrentFlowRunningNodeSize(Long flowId);


    /**
     * 查询当前flow运行中的任务数量
     * @param flowId
     * @return
     */
    List<ExecutionNodeEntity> queryNodeListByStateList(Long flowId, List<NodeExecuteStatusEnum> statusList);


    /**
     * 划动窗口
     * 查询接下来需要执行的任务列表，
     * @param flowId
     * @param curBatchId 当前batchId
     * @param jobStatus
     * @param requireExecCnt 任务数量
     * @return
     */
    List<ExecutionNodeEntity> queryNextRequireExecNodesList(Long flowId, Integer curBatchId,
                                     List<NodeExecuteStatusEnum> jobStatus, int requireExecCnt);


}
