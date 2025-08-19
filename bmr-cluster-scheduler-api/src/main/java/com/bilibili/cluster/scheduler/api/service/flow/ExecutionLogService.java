package com.bilibili.cluster.scheduler.api.service.flow;

import com.baomidou.mybatisplus.extension.service.IService;
import com.bilibili.cluster.scheduler.common.entity.ExecutionLogEntity;
import com.bilibili.cluster.scheduler.common.enums.flowLog.LogTypeEnum;

/**
 * <p>
 * 服务类
 * </p>
 *
 * @author 谢谢谅解
 * @since 2024-01-26
 */
public interface ExecutionLogService extends IService<ExecutionLogEntity> {

    /**
     * 查询日志
     *
     * @param executedId
     * @param logTypeEnum
     * @return
     */
    String queryLogByExecuteId(Long executedId, LogTypeEnum logTypeEnum);


    /**
     * 插入内容和新行
     *
     * @param executeId
     * @param execLogs
     * @return
     */
    void updateLogContent(Long executeId, LogTypeEnum logTypeEnum, String... execLogs);


    /**
     * 更新全局的事件日志
     * @param flowId
     * @param executionOrder    事件的顺序
     * @param execLogs
     */
    void updateGlobalEventLog(Long flowId, Integer executionOrder, String... execLogs);

    /**
     * 更新批次id
     * @param flowId
     * @param executionOrder
     * @param batchId
     * @param execLogs
     */
    void updateBatchEventLog(Long flowId,Integer executionOrder, Integer batchId, String... execLogs);


    /**
     * 查询日志id
     * @param executedId
     * @param logTypeEnum
     * @return
     */
    Long queryLogIdByExecuteId(Long executedId, LogTypeEnum logTypeEnum);

}
