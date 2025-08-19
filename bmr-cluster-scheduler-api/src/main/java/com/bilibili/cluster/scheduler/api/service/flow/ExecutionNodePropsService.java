package com.bilibili.cluster.scheduler.api.service.flow;

import com.baomidou.mybatisplus.extension.service.IService;
import com.bilibili.cluster.scheduler.common.entity.ExecutionNodePropsEntity;

import java.util.Collection;
import java.util.List;

/**
 * <p>
 * 服务类
 * </p>
 *
 * @author 谢谢谅解
 * @since 2024-01-26
 */
public interface ExecutionNodePropsService extends IService<ExecutionNodePropsEntity> {

    /**
     * 保存node的参数
     * @param nodeId
     * @param obj
     */
    void saveNodeProp(Long nodeId, Object obj);

    /**
     * 根据nodeId查询节点额外属性信息
     * @param nodeId
     * @param clazz
     * @param <T>
     * @return
     */
    <T> T queryNodePropsByNodeId(Long nodeId, Class<T> clazz);


    /**
     * 批量查询
     * @param nodeIdList
     * @param clazz
     * @param <T>
     * @return
     */
    <T> List<T> queryNodePropsByNodeIdList(Collection<Long> nodeIdList, Class<T> clazz);

}
