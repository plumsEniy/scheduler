package com.bilibili.cluster.scheduler.api.scheduler.cache;

import com.bilibili.cluster.scheduler.api.scheduler.runner.WorkflowInstanceExecuteRunnable;
import com.google.common.collect.ImmutableList;
import lombok.NonNull;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

/**
 * cache of process instance id and WorkflowExecuteThread
 */
@Component
public class ProcessInstanceExecCacheManagerImpl implements ProcessInstanceExecCacheManager {

    private final ConcurrentHashMap<Long, WorkflowInstanceExecuteRunnable> processInstanceExecMaps =
            new ConcurrentHashMap<>();


    @Override
    public WorkflowInstanceExecuteRunnable getByProcessInstanceId(Long processInstanceId) {
        return processInstanceExecMaps.get(processInstanceId);
    }

    @Override
    public boolean contains(Long processInstanceId) {
        return processInstanceExecMaps.containsKey(processInstanceId);
    }

    @Override
    public void removeByProcessInstanceId(Long processInstanceId) {
        processInstanceExecMaps.remove(processInstanceId);
    }

    @Override
    public void cache(Long processInstanceId, @NonNull WorkflowInstanceExecuteRunnable workflowExecuteThread) {
        processInstanceExecMaps.put(processInstanceId, workflowExecuteThread);
    }

    @Override
    public Collection<WorkflowInstanceExecuteRunnable> getAll() {
        return ImmutableList.copyOf(processInstanceExecMaps.values());
    }

    @Override
    public void clearCache() {
        processInstanceExecMaps.clear();
    }
}
