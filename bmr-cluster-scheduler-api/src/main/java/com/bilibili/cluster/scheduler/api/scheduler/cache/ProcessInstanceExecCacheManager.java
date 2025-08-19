package com.bilibili.cluster.scheduler.api.scheduler.cache;

import com.bilibili.cluster.scheduler.api.scheduler.runner.WorkflowInstanceExecuteRunnable;
import lombok.NonNull;

import java.util.Collection;

/**
 * cache of process instance id and WorkflowExecuteThread
 */
public interface ProcessInstanceExecCacheManager {

    /**
     * get WorkflowExecuteThread by process instance id
     *
     * @param processInstanceId processInstanceId
     * @return WorkflowExecuteThread
     */
    WorkflowInstanceExecuteRunnable getByProcessInstanceId(Long processInstanceId);

    /**
     * judge the process instance does it exist
     *
     * @param processInstanceId processInstanceId
     * @return true - if process instance id exists in cache
     */
    boolean contains(Long processInstanceId);

    /**
     * remove cache by process instance id
     *
     * @param processInstanceId processInstanceId
     */
    void removeByProcessInstanceId(Long processInstanceId);

    /**
     * cache
     *
     * @param processInstanceId     processInstanceId
     * @param workflowInstanceExecuteRunnable if it is null, will not be cached
     */
    void cache(Long processInstanceId, @NonNull WorkflowInstanceExecuteRunnable workflowInstanceExecuteRunnable);

    /**
     * get all WorkflowExecuteThread from cache
     *
     * @return all WorkflowExecuteThread in cache
     */
    Collection<WorkflowInstanceExecuteRunnable> getAll();

    void clearCache();
}
