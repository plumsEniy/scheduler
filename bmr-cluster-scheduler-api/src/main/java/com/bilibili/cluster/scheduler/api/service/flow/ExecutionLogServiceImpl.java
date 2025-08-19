package com.bilibili.cluster.scheduler.api.service.flow;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bilibili.cluster.scheduler.api.configuration.MasterConfig;
import com.bilibili.cluster.scheduler.common.Constants;
import com.bilibili.cluster.scheduler.common.entity.ExecutionFlowEntity;
import com.bilibili.cluster.scheduler.common.entity.ExecutionLogEntity;
import com.bilibili.cluster.scheduler.common.entity.ExecutionNodeEventEntity;
import com.bilibili.cluster.scheduler.common.enums.flowLog.LogTypeEnum;
import com.bilibili.cluster.scheduler.common.utils.LocalDateFormatterUtils;
import com.bilibili.cluster.scheduler.common.utils.NetUtils;
import com.bilibili.cluster.scheduler.dao.mapper.ExecutionLogMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 谢谢谅解
 * @since 2024-01-26
 */
@Service
public class ExecutionLogServiceImpl extends ServiceImpl<ExecutionLogMapper, ExecutionLogEntity> implements ExecutionLogService {

    @Resource
    MasterConfig masterConfig;

    @Resource
    private ExecutionLogMapper executionLogMapper;

    @Resource
    private ExecutionNodeEventService executionNodeEventService;

    @Resource
    private ExecutionFlowService executionFlowService;

    @Override
    public String queryLogByExecuteId(Long executedId, LogTypeEnum logTypeEnum) {
        ExecutionLogEntity executionLogEntity = executionLogMapper.queryByExecuteIdAndLogType(executedId, logTypeEnum);
        if (Objects.isNull(executionLogEntity)) {
            return Constants.EMPTY_STRING;
        }
        return executionLogEntity.getLogContent();
    }

    @Override
    public void updateLogContent(Long executeId, LogTypeEnum logTypeEnum, String... execLogs) {
        ExecutionLogEntity executionLogEntity = executionLogMapper.queryByExecuteIdAndLogType(executeId, logTypeEnum);
        StringBuffer logBuffer = new StringBuffer();
        if (executionLogEntity == null) {
            StringBuffer stringBuffer = appendLogMsg(logBuffer, execLogs);
            executionLogEntity = new ExecutionLogEntity();
            executionLogEntity.setExecuteId(executeId);
            executionLogEntity.setLogContent(stringBuffer.toString());
            executionLogEntity.setLogType(logTypeEnum);
            executionLogMapper.insert(executionLogEntity);
            return;
        }

        String logContent = executionLogEntity.getLogContent();
        logBuffer.append(logContent);
        StringBuffer stringBuffer = appendLogMsg(logBuffer, execLogs);
        executionLogEntity.setLogContent(stringBuffer.toString());
        executionLogMapper.updateById(executionLogEntity);
    }

    @Override
    public void updateGlobalEventLog(Long flowId, Integer executionOrder, String... execLogs) {
        ExecutionFlowEntity executionFlowEntity = executionFlowService.getById(flowId);
        Assert.isTrue(executionFlowEntity != null, String.format("can not find execution flow, flow id is %s", flowId));

        List<ExecutionNodeEventEntity> executionNodeEventList = executionNodeEventService.queryEventListByFlowIdExecuteOrder(flowId, executionOrder);
        for (ExecutionNodeEventEntity executionNodeEventEntity : executionNodeEventList) {
            updateLogContent(executionNodeEventEntity.getId(), LogTypeEnum.EVENT, execLogs);
        }
    }

    @Override
    public void updateBatchEventLog(Long flowId, Integer executionOrder, Integer batchId, String... execLogs) {
        ExecutionFlowEntity executionFlowEntity = executionFlowService.getById(flowId);
        Assert.isTrue(executionFlowEntity != null, String.format("can not find execution flow, flow id is %s", flowId));

        List<ExecutionNodeEventEntity> executionNodeEventList = executionNodeEventService.queryEventListByFlowIdExecuteOrderBatchId(flowId, batchId, executionOrder);
        for (ExecutionNodeEventEntity executionNodeEventEntity : executionNodeEventList) {
            updateLogContent(executionNodeEventEntity.getId(), LogTypeEnum.EVENT, execLogs);
        }
    }

    @Override
    public Long queryLogIdByExecuteId(Long executedId, LogTypeEnum logTypeEnum) {
        ExecutionLogEntity executionLogEntity = executionLogMapper.queryByExecuteIdAndLogType(executedId, logTypeEnum);
        if (Objects.isNull(executionLogEntity)) {
            return -1l;
        }
        return executionLogEntity.getId();
    }

    private StringBuffer appendLogMsg(StringBuffer logBuffer, String... execLogs) {
        Iterator<String> iterator = Arrays.stream(execLogs).iterator();
        while (iterator.hasNext()) {
            String execLog = iterator.next();
            logBuffer.append(String.format("%s -- %s: %s%s", LocalDateFormatterUtils.getNowDefaultFmt(), NetUtils.getAddr(masterConfig.getListenPort()), execLog, Constants.NEW_LINE));
            if (logBuffer.length() > Constants.MSG_MAX_SIZE) {
                logBuffer.delete(0, logBuffer.length() - Constants.MSG_MAX_SIZE);
            }
        }
        return logBuffer;
    }

}
