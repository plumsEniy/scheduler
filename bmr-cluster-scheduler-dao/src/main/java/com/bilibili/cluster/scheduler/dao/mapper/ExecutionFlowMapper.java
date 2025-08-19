package com.bilibili.cluster.scheduler.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bilibili.cluster.scheduler.common.dto.flow.UpdateExecutionFlowDTO;
import com.bilibili.cluster.scheduler.common.dto.flow.req.QueryFlowPageReq;
import com.bilibili.cluster.scheduler.common.dto.flow.req.spark.QuerySparkDeployFlowPageReq;
import com.bilibili.cluster.scheduler.common.dto.node.req.QueryHostExecutionFlowPageReq;
import com.bilibili.cluster.scheduler.common.entity.ExecutionFlowEntity;
import com.bilibili.cluster.scheduler.common.enums.flow.FlowStatusEnum;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * <p>
 * Mapper 接口
 * </p>
 *
 * @author 谢谢谅解
 * @since 2024-01-19
 */
public interface ExecutionFlowMapper extends BaseMapper<ExecutionFlowEntity> {

    /**
     * 更新flow状态
     */
    public void updateFlowStatusById(@Param("flowId") Long flowId, @Param("flowStatus") FlowStatusEnum flowStatus);


    public void updateFlow(@Param("updateExecutionFlowDTO") UpdateExecutionFlowDTO updateExecutionFlowDTO);

    /**
     * 分页查询工作流
     *
     * @param page
     * @param req
     * @return
     */
    IPage<ExecutionFlowEntity> selectPageList(Page<ExecutionFlowEntity> page, @Param("queryFlowPageReq") QueryFlowPageReq req);

    /**
     * 查询待执行的任务
     */
    List<ExecutionFlowEntity> findExecuteFlowPageBySlot(@Param("limit") int limit, @Param("offset") int offset,
                                                        @Param("masterCount") int masterCount,
                                                        @Param("thisMasterSlot") int thisMasterSlot);


    /**
     * 根据状态查询
     */
    List<ExecutionFlowEntity> findExecuteFlowByFlowStatus(@Param("flowStatusEnums") List<FlowStatusEnum> flowStatusEnums);

    /**
     * 更新flow失败节点个数
     */
    void updateCurFault(@Param("flowId") Long flowId, @Param("curFault") Integer curFault);

    /**
     * recovery场景
     * 返回待恢复执行实例主机列表
     */
    List<String> queryNeedFailoverFlowHost();


    /**
     * recovery场景
     * 根据主机名, 及状态查询需恢复执行的flow
     */
    List<ExecutionFlowEntity> queryNeedFailoverFlow(@Param("hostName") String hostName);

    /**
     * recovery场景
     * 修改flow执行实例主机名
     */
    void updateFlowHostName(@Param("hostName") String hostName, @Param("flowId") Long flowId);

    /**
     * 分页查询主机分页执行的工作流
     *
     * @param page
     * @param hostName
     * @return
     */
    IPage<ExecutionFlowEntity> queryHostExecutionFlowPage(Page<ExecutionFlowEntity> page, @Param("req") QueryHostExecutionFlowPageReq req);

    /**
     *
     * @param flowId
     * @param maxBatchId
     */
    void updateFlowMaxBatchId( @Param("flowId") Long flowId,  @Param("maxBatchId") Integer maxBatchId);

    /**
     * 分页查询spark发布单列表信息
     * @param page
     * @param req
     * @return
     */
    IPage selectSparkDeployPageList(Page<ExecutionFlowEntity> page, @Param("req") QuerySparkDeployFlowPageReq req);

}

