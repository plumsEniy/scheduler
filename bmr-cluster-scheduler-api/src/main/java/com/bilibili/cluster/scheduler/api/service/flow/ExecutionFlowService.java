package com.bilibili.cluster.scheduler.api.service.flow;


import com.baomidou.mybatisplus.extension.service.IService;
import com.bilibili.cluster.scheduler.common.dto.flow.ExecutionFlowInstanceDTO;
import com.bilibili.cluster.scheduler.common.dto.flow.UpdateExecutionFlowDTO;
import com.bilibili.cluster.scheduler.common.dto.flow.req.DeployOneFlowReq;
import com.bilibili.cluster.scheduler.common.dto.flow.req.QueryFlowListReq;
import com.bilibili.cluster.scheduler.common.dto.flow.req.QueryFlowPageReq;
import com.bilibili.cluster.scheduler.common.dto.flow.req.spark.QuerySparkDeployFlowPageReq;
import com.bilibili.cluster.scheduler.common.dto.node.req.QueryHostExecutionFlowPageReq;
import com.bilibili.cluster.scheduler.common.entity.ExecutionFlowEntity;
import com.bilibili.cluster.scheduler.common.enums.flow.FlowOperateButtonEnum;
import com.bilibili.cluster.scheduler.common.enums.flow.FlowRollbackType;
import com.bilibili.cluster.scheduler.common.enums.flow.FlowStatusEnum;
import com.bilibili.cluster.scheduler.common.response.ResponseResult;

import java.util.List;

/**
 * <p>
 * 服务类
 * </p>
 *
 * @author 谢谢谅解
 * @since 2024-01-19
 */
public interface ExecutionFlowService extends IService<ExecutionFlowEntity> {

    /**
     * 查询待执行的flow
     */
    List<ExecutionFlowEntity> findExecuteFlowPageBySlot(int pageSize, int pageNumber, int masterCount, int thisMasterSlot);


    /**
     * 根据状态查询
     */
    List<ExecutionFlowEntity> findExecuteFlowByFlowStatus(List<FlowStatusEnum> flowStatusEnums);

    /**
     * 根据flowId 更新flow状态
     */
    void updateFlowStatusByFlowId(Long flowId, FlowStatusEnum flowStatusEnum);

    public void updateFlow(UpdateExecutionFlowDTO updateExecutionFlowDTO);

    void updateFlowStatusAndCurrentBatchIdByFlowId(Long flowId, FlowStatusEnum flowStatusEnum, Integer currentBatchId);

    /**
     * 处理flow
     */
    public ExecutionFlowInstanceDTO handleWorkFlowProcess(String host, ExecutionFlowEntity executionFlowEntity) throws Exception;


    /**
     * 分页查询工作流
     *
     * @param req
     * @return
     */
    public ResponseResult queryFlowPage(QueryFlowPageReq req);

    /**
     * 查询flow运行时数据，包括成功节点个数，日志以及按钮
     *
     * @param flowId
     * @return
     */
    ResponseResult getFlowRuntimeData(Long flowId);

    /**
     * 变更flow运行状态
     *
     * @param flowId
     * @param operate
     * @return
     */
    ResponseResult alterFlowStatus(long flowId, FlowOperateButtonEnum operate) throws Exception;

    /**
     * 查询工作流详情
     *
     * @param flowId
     * @return
     */
    ResponseResult queryFlowInfo(Long flowId);

    /**
     * 更新当前失败节点个数
     */
    void updateCurFault(Long flowId, Integer curFault);


    /**
     * 创建flowinstance
     * 核心生成InstanceId
     * @param host
     * @param executionFlowEntity
     * @param curBatchId
     * @return
     */
    ExecutionFlowInstanceDTO generateExecutionFlowInstance(String host, ExecutionFlowEntity executionFlowEntity, Integer curBatchId);

    /**
     * 分页查询主机工作流
     *
     * @param req
     * @return
     */
    ResponseResult queryHostExecutionFlowPage(QueryHostExecutionFlowPageReq req);


    /**
     * 执行工作流
     *
     * @param flowId
     * @return
     */
    ResponseResult executionOneFlow(Long flowId);


    /**
     * 取消工作流
     * @param flowId
     * @return
     */
    ResponseResult cancelFlow(Long flowId);

    ResponseResult applyFlow(DeployOneFlowReq req);

    void updateMaxBatchId(Long flowId, Integer maxBatchId);

    /**
     * 查询pipeline定义
     * @param flowId
     * @return
     */
    ResponseResult queryPipelineDefine(Long flowId) throws Exception;

    /**
     *
     * @param alreadyWaitFlowList
     * @return
     */
    List<ExecutionFlowEntity> queryPrepareFlowList(List<Long> alreadyWaitFlowList);

    /**
     * 查询spark发布单,分页接口
     * @param req
     * @return
     */
    ResponseResult querySparkDeployFlowPageList(QuerySparkDeployFlowPageReq req);

    /**
     * 查询spark发布单列表，根据特定任务id查询
     * @param jobId
     * @return
     */
    ResponseResult querySparkDeployFlowListByJobId(String jobId);

    /**
     * 事务序列化读
     * @param flowId
     * @return
     */
    ExecutionFlowEntity queryByIdWithTransactional(Long flowId);

    /**
     * 查询工作流列表
     * @param req
     * @return
     */
    ResponseResult queryFlowInfoList(QueryFlowListReq req);

    /**
     * 根据主机名称查询spark客户端发布历史信息
     * @param nodeName
     * @return
     */
    ResponseResult querySparkClientDeployInfoByNodeName(String nodeName);

    /**
     * 软删除flow信息
     * @param flowId
     * @return
     */
    ResponseResult deleteFlowById(Long flowId);

    /**
     * @param flowId
     * @param flowState
     * @return
     */
    ResponseResult adminModifyFlowStatus(Long flowId, String flowState);

    List<String> getOpAdminList();

    boolean updateFlowTolerance(Long flowId, int tolerance);

    // 设置回滚范围
    boolean updateFlowRollbackType(FlowRollbackType rollbackType, long flowId);

    boolean updateCurrentBatchIdByFlowId(Long flowId, Integer batchId);

    /**
     * 生成flow的url
     * @param flowId
     * @return
     */
    String generateFlowUrl(Long flowId);

    /**
     * 生成flow的url
     * @param executionFlow
     * @return
     */
    String generateFlowUrl(ExecutionFlowEntity executionFlow);
}
