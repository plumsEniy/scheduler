package com.bilibili.cluster.scheduler.api.tools;

import com.bilibili.cluster.scheduler.common.Constants;
import com.bilibili.cluster.scheduler.common.dto.button.DeployStageInfo;
import com.bilibili.cluster.scheduler.common.dto.button.StageStateEnum;
import com.bilibili.cluster.scheduler.common.dto.button.StageStatePoint;
import com.bilibili.cluster.scheduler.common.dto.flow.ExecutionFlowRuntimeDataDTO;
import com.bilibili.cluster.scheduler.common.dto.flow.prop.BaseFlowExtPropDTO;
import com.bilibili.cluster.scheduler.common.dto.node.dto.ExecutionNodeSummary;
import com.bilibili.cluster.scheduler.common.entity.ExecutionFlowEntity;
import com.bilibili.cluster.scheduler.common.entity.ExecutionNodeEntity;
import com.bilibili.cluster.scheduler.common.enums.NodeExecuteStatusEnum;
import com.bilibili.cluster.scheduler.common.enums.flow.FlowDeployType;
import com.bilibili.cluster.scheduler.common.enums.flow.FlowStatusEnum;
import com.bilibili.cluster.scheduler.common.enums.node.NodeType;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;

/**
 * @description: flow信息填充
 * @Date: 2024/2/1 16:27
 * @Author: nizhiqiang
 */
public class FlowDetailAdaptor {

    /**
     * 计算成功节点和失败节点数，结果填充进flow
     *
     * @param executionNodeSummaryList
     */
    public static void statisticalStatus(ExecutionFlowEntity executionFlow, List<ExecutionNodeSummary> executionNodeSummaryList,
                                         ExecutionFlowRuntimeDataDTO flowRuntimeData, ExecutionNodeEntity curNodeEntity, BaseFlowExtPropDTO baseFlowExtPropDTO) {
        int totalCnt = 0, successCnt = 0, rollbackCnt = 0, runningCnt = 0, checkingCnt = 0, failedCnt = 0,
                skipCnt = 0, originCnt = 0, finishCnt = 0, unExecutionCnt = 0;

        if (Objects.isNull(curNodeEntity)) {
            return;
        }
        if (CollectionUtils.isEmpty(executionNodeSummaryList)) {
            return;
        }
        String curStage = curNodeEntity.getExecStage();
        int size = executionNodeSummaryList.size();
        ExecutionNodeSummary lastNodeExecutionNodeSummary = executionNodeSummaryList.get(size - 1);
        String totalStage = lastNodeExecutionNodeSummary.getExecStage();

        Map<String, Set<NodeExecuteStatusEnum>> stageWithStateMap = new LinkedHashMap<>();
        Map<String, Long> stageWithPreAllCount = new LinkedHashMap<>();

        for (ExecutionNodeSummary executionNodeSummary : executionNodeSummaryList) {
            stageWithStateMap.computeIfAbsent(executionNodeSummary.getExecStage(), k -> new HashSet<>()).add(executionNodeSummary.getExecuteStatus());

            long curCount = executionNodeSummary.getCount();

            totalCnt += curCount;
            stageWithPreAllCount.put(executionNodeSummary.getExecStage(), new Long(totalCnt));
            NodeExecuteStatusEnum jobStatus = executionNodeSummary.getExecuteStatus();
            switch (jobStatus) {
                case UN_NODE_EXECUTE:
                case UN_NODE_RETRY_EXECUTE:
                    originCnt += curCount;
                    break;
                case UN_NODE_ROLLBACK_EXECUTE:
                    successCnt += curCount;
                    break;
                case IN_NODE_EXECUTE:
                case IN_NODE_RETRY_EXECUTE:
                case IN_NODE_ROLLBACK_EXECUTE:
                    runningCnt += curCount;
                    break;
                case FAIL_NODE_EXECUTE:
                case FAIL_SKIP_NODE_EXECUTE:
                case FAIL_NODE_RETRY_EXECUTE:
                case FAIL_NODE_ROLLBACK_EXECUTE:
                case FAIL_SKIP_NODE_RETRY_EXECUTE:
                case FAIL_SKIP_NODE_ROLLBACK_EXECUTE:
                    originCnt += curCount;
                    failedCnt += curCount;
                    break;
                case SKIPPED:
                    skipCnt+= curCount;
                    successCnt += curCount;
                    break;
                case SUCCEED_NODE_EXECUTE:
                case SUCCEED_NODE_RETRY_EXECUTE:
                    successCnt += curCount;
                    finishCnt += curCount;
                    break;
                case SUCCEED_NODE_ROLLBACK_EXECUTE:
                case ROLLBACK_SKIPPED:
                    skipCnt+= curCount;
                    originCnt += curCount;
                    rollbackCnt += curCount;
            }
        }
//        任务总数
        flowRuntimeData.setTotalCnt(totalCnt);
//        任务成功数量（执行成功+回滚成功+跳过）
        flowRuntimeData.setSuccessCnt(successCnt);
//        跳过节点数
        flowRuntimeData.setSkipCnt(skipCnt);
//        回滚节点数
        flowRuntimeData.setRollbackCnt(rollbackCnt);
//        正在运行数
        flowRuntimeData.setRollbackCnt(runningCnt);
//        执行失败数
        flowRuntimeData.setFailedCnt(failedCnt);
//        原版本
        flowRuntimeData.setOriginCnt(originCnt);
//        新版本
        flowRuntimeData.setFinishCnt(finishCnt);
//        未执行数量
        unExecutionCnt = totalCnt - successCnt;
        flowRuntimeData.setUnExecutionCnt(unExecutionCnt);

        if (StringUtils.isBlank(curStage)) {
            curStage = Constants.DEFAULT_EXEC_STAGE;
        }
        if (StringUtils.isBlank(totalStage)) {
            totalStage = Constants.DEFAULT_EXEC_STAGE;
        }
        flowRuntimeData.setCurStage(curStage);
        flowRuntimeData.setTotalStage(totalStage);

        DecimalFormat df = new DecimalFormat("#0.00");
        String successPercent = df.format(successCnt * 100.0 / totalCnt);
        String unSuccessPercent = df.format((totalCnt - successCnt) * 100.0 / totalCnt);

        flowRuntimeData.setSuccessPercent(successPercent + Constants.PERCENT_SIGNAL);
        flowRuntimeData.setUnSuccessPercent(unSuccessPercent + Constants.PERCENT_SIGNAL);

        List<StageStatePoint> stageStatePointList = new ArrayList<>();
        flowRuntimeData.setStageStatePointList(stageStatePointList);

        int stageSize = stageWithStateMap.size();
        for (Map.Entry<String, Set<NodeExecuteStatusEnum>> entry : stageWithStateMap.entrySet()) {
            StageStatePoint statePoint = new StageStatePoint();
            stageStatePointList.add(statePoint);
            final String stage = entry.getKey();
            int stageValue;
            if (StringUtils.isBlank(stage)) {
                stageValue = 1;
            } else {
                stageValue = Integer.parseInt(stage);
            }
            statePoint.setId(stageValue);
            Set<NodeExecuteStatusEnum> statusEnumSet = entry.getValue();

            if (stage.compareTo(curStage) < 0) {
                if (statusEnumSet.contains(NodeExecuteStatusEnum.FAIL_NODE_EXECUTE) || statusEnumSet.contains(NodeExecuteStatusEnum.FAIL_NODE_RETRY_EXECUTE)) {
                    statePoint.setStatus(StageStateEnum.FAIL);
                } else {
                    statePoint.setStatus(StageStateEnum.SUCCESS);
                }
            } else if (stage.compareTo(curStage) > 0) {
                statePoint.setStatus(StageStateEnum.UN_EXECUTE);
            } else {
//                if (statusEnumSet.contains(NodeExecuteStatusEnum.FAIL_NODE_EXECUTE) || statusEnumSet.contains(NodeExecuteStatusEnum.FAIL_NODE_RETRY_EXECUTE)) {
//                    statePoint.setStatus(StageStateEnum.FAIL);
//                } else if (statusEnumSet.contains(NodeExecuteStatusEnum.IN_NODE_EXECUTE) || statusEnumSet.contains(NodeExecuteStatusEnum.IN_NODE_RETRY_EXECUTE)) {
//                    statePoint.setStatus(StageStateEnum.RUNNING);
//                } else if (statusEnumSet.contains(NodeExecuteStatusEnum.UN_NODE_EXECUTE)) {
//                    statePoint.setStatus(StageStateEnum.UN_EXECUTE);
//                } else {
//                    statePoint.setStatus(StageStateEnum.SUCCESS);
//                }
                if (NodeType.STAGE_START_NODE.equals(curNodeEntity.getNodeType())) {
                    statePoint.setStatus(StageStateEnum.UN_EXECUTE);
                } else if (NodeType.STAGE_END_NODE.equals(curNodeEntity.getNodeType())) {
                    statePoint.setStatus(StageStateEnum.SUCCESS);
                } else {
                    final FlowStatusEnum flowStatus = executionFlow.getFlowStatus();
                    switch (flowStatus) {
                        case SUCCEED_EXECUTE:
                        case TERMINATE:
                        case ROLLBACK_SUCCESS:
                            statePoint.setStatus(StageStateEnum.SUCCESS);
                            break;
                        case UNDER_APPROVAL:
                        case APPROVAL_NOT_PASS:
                        case APPROVAL_PASS:
                        case APPROVAL_FAIL:
                        case PREPARE_EXECUTE:
                        case PREPARE_EXECUTE_FAILED:
                        case UN_EXECUTE:
                        case CANCEL:
                            statePoint.setStatus(StageStateEnum.UN_EXECUTE);
                            break;
                        case PAUSED:
                        case ROLLBACK_PAUSED:
                        case IN_EXECUTE:
                        case IN_ROLLBACK:
                            statePoint.setStatus(StageStateEnum.RUNNING);
                            break;
                        case FAIL_EXECUTE:
                        case ROLLBACK_FAILED:
                            statePoint.setStatus(StageStateEnum.FAIL);
                            break;
                    }
                }
            }

            String content = getStageContent(executionFlow, stageValue, stageSize,
                    getStagePercent(stageWithPreAllCount.getOrDefault(stage, 0l), totalCnt));
            statePoint.setContent(content);

            if (Objects.isNull(baseFlowExtPropDTO)) {
                continue;
            }
            Map<Integer, DeployStageInfo> stageInfos = baseFlowExtPropDTO.getStageInfos();
            if (MapUtils.isEmpty(stageInfos)) {
                continue;
            }
            final DeployStageInfo stageInfo = stageInfos.get(stageValue);
            if (Objects.isNull(stageInfo)) {
                continue;
            }
            statePoint.setStartTime(stageInfo.getStartTime());
            statePoint.setEndTime(stageInfo.getEndTime());
            statePoint.setAllowedNextStageStartTime(stageInfo.getAllowedNextStageStartTime());

            final StageStateEnum state = stageInfo.getState();
            if (!Objects.isNull(state) && state.isFinishState()) {
                statePoint.setStatus(state);
            }
        }
    }

    private static Supplier<String> getStagePercent(long curPos, long total) {
        return () -> {
            try {
                DecimalFormat df = new DecimalFormat("#0.0");
                String percent = df.format(curPos * 100.0 / total);
                return percent;
            } catch (Exception e) {
                return "未知数量";
            }
        };
    }

    private static String getStageContent(ExecutionFlowEntity executionFlow, int stageValue, int stageSize,
                                          Supplier<String> percent) {
        String content = null;
        if (stageSize == stageValue) {
            content = String.format("第%s阶段", stageValue) + "（100%）";
            return content;
        }
        final FlowDeployType deployType = executionFlow.getDeployType();
        if (stageSize == 7) {
            switch (stageValue) {
                case 1:
                    content = "第1阶段（1%）";
                    break;
                case 2:
                    content = "第2阶段（10%）";
                    break;
                case 3:
                    content = "第3阶段（30%）";
                    break;
                case 4:
                    content = "第4阶段（50%）";
                    break;
                case 5:
                    content = "第5阶段（65%）";
                    break;
                case 6:
                    content = "第6阶段（96%）";
                    break;
                case 7:
                    content = "第7阶段（100%）";
                    break;
            }

        } else if (stageSize == 5) {
            switch (stageValue) {
                case 1:
                    content = "第1阶段（1%）";
                    break;
                case 2:
                    content = "第2阶段（10%）";
                    break;
                case 3:
                    content = "第3阶段（20%）";
                    break;
                case 4:
                    content = "第4阶段（50%）";
                    break;
                case 5:
                    content = "第5阶段（100%）";
                    break;
            }
        }

        if (StringUtils.isBlank(content)) {
            content = "第" + stageValue + "阶段（" + percent.get() + "%）";
        }
        return content;
    }
}
