package com.bilibili.cluster.scheduler.api.service.flow;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bilibili.cluster.scheduler.api.event.flow.AbstractFlowAopEventHandler;
import com.bilibili.cluster.scheduler.common.entity.ExecutionFlowAopEventEntity;
import com.bilibili.cluster.scheduler.common.entity.ExecutionFlowEntity;
import com.bilibili.cluster.scheduler.common.entity.ExecutionNodeEntity;
import com.bilibili.cluster.scheduler.common.enums.flow.FlowAopEventType;
import com.bilibili.cluster.scheduler.common.enums.flow.FlowDeployType;
import com.bilibili.cluster.scheduler.common.enums.flow.FlowStatusEnum;
import com.bilibili.cluster.scheduler.dao.mapper.ExecutionFlowAopEventMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * <p>
 * 工作流切片事件 服务实现类
 * </p>
 *
 * @author 谢谢谅解
 * @since 2025-05-12
 */
@Service
public class ExecutionFlowAopEventServiceImpl extends ServiceImpl<ExecutionFlowAopEventMapper, ExecutionFlowAopEventEntity> implements ExecutionFlowAopEventService {

    @Resource
    private List<AbstractFlowAopEventHandler> flowAopEventHandlerList;


    private Map<FlowAopEventType, AbstractFlowAopEventHandler> flowAopEventHandlerMap;

    @PostConstruct
    public void init() {
        flowAopEventHandlerMap = flowAopEventHandlerList.stream()
                .collect(Collectors.toMap(AbstractFlowAopEventHandler::getEventType, Function.identity()));
    }

    @Override
    public void initFlowAopEvent(ExecutionFlowEntity flowEntity) {
        FlowDeployType deployType = flowEntity.getDeployType();

        List<FlowAopEventType> executionFlowAopEventList = new LinkedList<>();

        if (deployType.isFailNotify()) {
            executionFlowAopEventList.add(FlowAopEventType.JOB_FAIL_NOTIFY);
        }

        if (deployType.isIncident()) {
            executionFlowAopEventList.add(FlowAopEventType.INCIDENT);
        }

        if (deployType.isRefreshResource()) {
            executionFlowAopEventList.add(FlowAopEventType.REFRESH_RESOURCE);
        }

        if (CollectionUtils.isEmpty(executionFlowAopEventList)) {
            return;
        }

        LinkedList<ExecutionFlowAopEventEntity> executionFlowAopEventEntityList = new LinkedList<>();
        for (FlowAopEventType flowAopEventType : executionFlowAopEventList) {
            ExecutionFlowAopEventEntity executionFlowAopEventEntity = new ExecutionFlowAopEventEntity();
            executionFlowAopEventEntity.setFlowId(flowEntity.getId());
            executionFlowAopEventEntity.setEventType(flowAopEventType);
            executionFlowAopEventEntity.setCount(0);
            executionFlowAopEventEntityList.add(executionFlowAopEventEntity);
        }
        saveBatch(executionFlowAopEventEntityList);
    }

    @Override
    public void createFlowAop(ExecutionFlowEntity flowEntity) {
        executeFlowAopHandler(flowEntity, handler -> handler.createFlow(flowEntity));
    }

    @Override
    public void startFlowAop(ExecutionFlowEntity flowEntity) {
        executeFlowAopHandler(flowEntity, handler -> handler.startFlow(flowEntity));
    }

    @Override
    public void giveUpFlowAop(ExecutionFlowEntity flowEntity) {
        executeFlowAopHandler(flowEntity, handler -> handler.giveUpFlow(flowEntity));
    }

    @Override
    public void jobFailAop(ExecutionFlowEntity flowEntity, ExecutionNodeEntity executionNode, String errorMsg) {
        executeFlowAopHandler(flowEntity, handler -> handler.jobFail(flowEntity, executionNode, errorMsg));
    }

    @Override
    public void jobFinishAop(ExecutionFlowEntity flowEntity, ExecutionNodeEntity executionNode) {
        executeFlowAopHandler(flowEntity, handler -> handler.jobFinish(flowEntity, executionNode));
    }

    @Override
    public void finishFlowAop(ExecutionFlowEntity flowEntity, FlowStatusEnum beforeStatus) {
        executeFlowAopHandler(flowEntity, handler -> handler.finishFlow(flowEntity, beforeStatus));
    }


    /**
     * 执行流程AOP处理程序
     * 本方法用于处理特定流程中的所有AOP事件它首先根据流程ID获取所有相关事件，
     * 然后遍历每个事件并使用提供的函数进行处理如果事件处理过程中发生异常，它将记录错误日志
     *
     * @param flowEntity 流程实体，包含流程的基本信息
     * @param function   处理事件的函数，接受一个AbstractFlowAopEventHandler类型的参数
     */
    private void executeFlowAopHandler(ExecutionFlowEntity flowEntity, Function<AbstractFlowAopEventHandler, Boolean> function) {
        Long flowId = flowEntity.getId();
        List<ExecutionFlowAopEventEntity> eventEntityList = getEventListByFlowId(flowId);
        if (CollectionUtils.isEmpty(eventEntityList)) {
            return;
        }

        for (ExecutionFlowAopEventEntity eventEntity : eventEntityList) {
            FlowAopEventType eventType = eventEntity.getEventType();
            try {
                AbstractFlowAopEventHandler eventHandler = flowAopEventHandlerMap.get(eventType);
//                为false代表没重写，默认不加计数器
                Boolean isExecute = function.apply(eventHandler);
                if (isExecute) {
                    updateEventCount(eventEntity.getId());
                }
            } catch (Exception e) {
                String errorMsg = String.format("执行工作流切片事件失败，flowId=%s, eventType=%s", flowId, eventType.getDesc());
                log.error(errorMsg, e);
            }
        }
    }

    private void updateEventCount(Long eventId) {
        LambdaUpdateWrapper<ExecutionFlowAopEventEntity> updateWrapper = new LambdaUpdateWrapper<ExecutionFlowAopEventEntity>()
                .eq(ExecutionFlowAopEventEntity::getId, eventId)
                .setSql("count = count + 1");
        update(updateWrapper);
    }

    private List<ExecutionFlowAopEventEntity> getEventListByFlowId(Long flowId) {
        LambdaQueryWrapper<ExecutionFlowAopEventEntity> queryWrapper = new LambdaQueryWrapper<ExecutionFlowAopEventEntity>()
                .eq(ExecutionFlowAopEventEntity::getFlowId, flowId);
        List<ExecutionFlowAopEventEntity> eventList = list(queryWrapper);
        return eventList;
    }
}
