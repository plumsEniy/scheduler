package com.bilibili.cluster.scheduler.api.event.factory;

import cn.hutool.core.collection.CollectionUtil;
import com.bilibili.cluster.scheduler.api.bean.SpringApplicationContext;
import com.bilibili.cluster.scheduler.api.event.analyzer.PipelineParameter;
import com.bilibili.cluster.scheduler.api.event.analyzer.ResolvedEvent;
import com.bilibili.cluster.scheduler.api.event.analyzer.UnResolveEvent;
import com.bilibili.cluster.scheduler.api.exceptions.DolphinSchedulerInvokerException;
import com.bilibili.cluster.scheduler.api.service.scheduler.DolphinSchedulerInteractService;
import com.bilibili.cluster.scheduler.common.dolphin.DolphinPipelineDefinition;
import com.bilibili.cluster.scheduler.common.dolphin.DolphinPipelineResolveParameter;
import com.bilibili.cluster.scheduler.common.dto.scheduler.model.PipelineDefine;
import com.bilibili.cluster.scheduler.common.dto.scheduler.model.SchedTaskDefine;
import com.bilibili.cluster.scheduler.common.enums.dolphin.TaskPosType;
import com.bilibili.cluster.scheduler.common.enums.event.EventReleaseScope;
import com.bilibili.cluster.scheduler.common.enums.event.EventTypeEnum;
import com.google.common.base.Preconditions;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@Slf4j
public abstract class AbstractPipelineFactory implements PipelineFactory {

    protected DolphinSchedulerInteractService dolphinSchedulerInteractService;

    public AbstractPipelineFactory() {
        abstractPipelineFactoryInit();
    }

    protected void abstractPipelineFactoryInit() {
        dolphinSchedulerInteractService = SpringApplicationContext.getBean(DolphinSchedulerInteractService.class);
    }

    @Override
    public List<ResolvedEvent> analyzerAndResolveEvents(PipelineParameter pipelineParameter) throws Exception {
        List<UnResolveEvent> unResolveEvents = analyzer(pipelineParameter);
        log.info("PipelineFactory parse unResolveEvents is {}", unResolveEvents);
        Preconditions.checkNotNull(unResolveEvents, "analyzedEvents is null");
        Preconditions.checkState(CollectionUtil.isNotEmpty(unResolveEvents), "analyzedEvents is empty");

        List<ResolvedEvent> resolvedEvents = new ArrayList<>();
        for (UnResolveEvent unResolveEvent : unResolveEvents) {
            List<ResolvedEvent> resolvedEventList = resolve(unResolveEvent, pipelineParameter);
            resolvedEvents.addAll(resolvedEventList);
        }
        return resolvedEvents;
    }

    @Override
    public List<ResolvedEvent> resolve(UnResolveEvent unResolveEvent, PipelineParameter pipelineParameter) throws Exception {
        Preconditions.checkNotNull(unResolveEvent, "analyzedEvent is null");
        final EventTypeEnum eventType = unResolveEvent.getEventTypeEnum();
        if (eventType.isDolphinType()) {
            return resolveDolphinPipelineEvent(unResolveEvent, pipelineParameter);
        } else {
            return Arrays.asList(resolveCommonEvent(unResolveEvent));
        }
    }

    private List<ResolvedEvent> resolveDolphinPipelineEvent(UnResolveEvent unResolveEvent, PipelineParameter pipelineParameter) throws DolphinSchedulerInvokerException {
        DolphinPipelineDefinition pipelineDefinition;

        final String projectCode = unResolveEvent.getProjectCode();
        final String pipelineCode = unResolveEvent.getPipelineCode();
        if (!StringUtils.isBlank(projectCode) && !StringUtils.isBlank(pipelineCode)) {
            List<SchedTaskDefine> taskDefineList = dolphinSchedulerInteractService.parsePipelineDefineByCode(projectCode, pipelineCode);
            pipelineDefinition = new DolphinPipelineDefinition();
            pipelineDefinition.setProjectCode(projectCode);
            pipelineDefinition.setPipelineCode(pipelineCode);
            pipelineDefinition.setSchedTaskDefineList(taskDefineList);
        } else {
            DolphinPipelineResolveParameter parameter = new DolphinPipelineResolveParameter();
            parameter.setRoleName(pipelineParameter.getFlowEntity().getRoleName());
            parameter.setClusterName(pipelineParameter.getClusterData().getClusterName());
            // set cluster alias
            parameter.setClusterAlias(pipelineParameter.getClusterData().getAnotherName());
            parameter.setComponentName(pipelineParameter.getComponentData().getComponentName());
            parameter.setFlowDeployType(pipelineParameter.getFlowEntity().getDeployType());
            parameter.setChainedIndex(unResolveEvent.getGroupIndex());
            pipelineDefinition = dolphinSchedulerInteractService.resolvePipelineDefinition(parameter);
        }

        log.info("for resolveDolphinPipelineEvent pipelineDefinition is {}", pipelineDefinition);
        List<SchedTaskDefine> schedTaskDefineList = pipelineDefinition.getSchedTaskDefineList();
        List<ResolvedEvent> resolvedEvents = new ArrayList<>();

        String startTaskCode = schedTaskDefineList.get(0).getTaskCode();
        String endTaskCode = schedTaskDefineList.get(schedTaskDefineList.size() -1).getTaskCode();

        for (SchedTaskDefine taskDefine : schedTaskDefineList) {
            String taskCode = taskDefine.getTaskCode();
            String taskName = taskDefine.getName();
            ResolvedEvent resolvedEvent = new ResolvedEvent();
            BeanUtils.copyProperties(unResolveEvent, resolvedEvent);
            resolvedEvent.setProjectCode(pipelineDefinition.getProjectCode());
            resolvedEvent.setPipelineCode(pipelineDefinition.getPipelineCode());
            resolvedEvent.setTaskCode(taskCode);
            resolvedEvent.setEventName(taskName);
            resolvedEvent.setFailureStrategy(unResolveEvent.getFailureStrategy());

            if (taskCode.equals(startTaskCode) && taskCode.equals(endTaskCode)) {
                resolvedEvent.setTaskPosType(TaskPosType.DOLPHIN_SINGLE_NODE);
            } else if (taskCode.equals(startTaskCode)) {
                resolvedEvent.setTaskPosType(TaskPosType.DOLPHIN_START_NODE);
            } else if (taskCode.equals(endTaskCode)) {
                resolvedEvent.setTaskPosType(TaskPosType.DOLPHIN_END_NODE);
            } else {
                resolvedEvent.setTaskPosType(TaskPosType.DOLPHIN_INTERMEDIATE_NODE);
            }
            resolvedEvents.add(resolvedEvent);
        }
        return resolvedEvents;
    }

    protected ResolvedEvent resolveCommonEvent(UnResolveEvent unResolveEvent) {
        ResolvedEvent resolvedEvent = new ResolvedEvent();
        BeanUtils.copyProperties(unResolveEvent, resolvedEvent);
        resolvedEvent.setEventName(unResolveEvent.getEventTypeEnum().getDesc());
        if (Objects.isNull(resolvedEvent.getScope())) {
            // 默认解析至事件级别
            resolvedEvent.setScope(EventReleaseScope.EVENT);
        }
        return resolvedEvent;
    }

}
