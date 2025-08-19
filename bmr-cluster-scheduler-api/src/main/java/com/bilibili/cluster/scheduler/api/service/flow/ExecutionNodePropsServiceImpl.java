package com.bilibili.cluster.scheduler.api.service.flow;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bilibili.cluster.scheduler.common.Constants;
import com.bilibili.cluster.scheduler.common.entity.ExecutionNodePropsEntity;
import com.bilibili.cluster.scheduler.dao.mapper.ExecutionNodePropsMapper;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
public class ExecutionNodePropsServiceImpl extends ServiceImpl<ExecutionNodePropsMapper, ExecutionNodePropsEntity> implements ExecutionNodePropsService {


    @Override
    public void saveNodeProp(Long nodeId, Object obj) {
        LambdaQueryWrapper<ExecutionNodePropsEntity> queryWrapper = new QueryWrapper<ExecutionNodePropsEntity>().lambda();
        queryWrapper.eq(ExecutionNodePropsEntity::getNodeId, nodeId).orderByDesc(ExecutionNodePropsEntity::getId).last(Constants.LIMIT_ONE);
        ExecutionNodePropsEntity executionFlowPropsEntity = getOne(queryWrapper);
        if (executionFlowPropsEntity == null) {
            executionFlowPropsEntity = new ExecutionNodePropsEntity();
            executionFlowPropsEntity.setNodeId(nodeId);
            executionFlowPropsEntity.setPropsContent(JSONUtil.toJsonStr(obj));
            save(executionFlowPropsEntity);
            return;
        }

        LambdaUpdateWrapper<ExecutionNodePropsEntity> updateWrapper = new UpdateWrapper<ExecutionNodePropsEntity>().lambda();
        updateWrapper.eq(ExecutionNodePropsEntity::getNodeId, nodeId)
                .set(ExecutionNodePropsEntity::getPropsContent, JSONUtil.toJsonStr(obj));
        update(updateWrapper);
    }

    @Override
    public <T> T queryNodePropsByNodeId(Long nodeId, Class<T> clazz) {
        LambdaQueryWrapper<ExecutionNodePropsEntity> queryWrapper = new QueryWrapper<ExecutionNodePropsEntity>().lambda()
                .eq(ExecutionNodePropsEntity::getNodeId, nodeId)
                .orderByDesc(ExecutionNodePropsEntity::getId)
                .last(Constants.LIMIT_ONE);
        ExecutionNodePropsEntity nodePropsEntity = getOne(queryWrapper);

        if (Objects.isNull(nodePropsEntity)) {
            return null;
        }
        String propsContent = nodePropsEntity.getPropsContent();
        if (StringUtils.isBlank(propsContent)) {
            return null;
        }
        return JSONUtil.toBean(propsContent, clazz);
    }

    @Override
    public <T> List<T> queryNodePropsByNodeIdList(Collection<Long> nodeIdList, Class<T> clazz) {
        if (CollectionUtils.isEmpty(nodeIdList)) {
            return Collections.EMPTY_LIST;
        }
        LambdaQueryWrapper<ExecutionNodePropsEntity> queryWrapper = new QueryWrapper<ExecutionNodePropsEntity>().lambda()
                .in(ExecutionNodePropsEntity::getNodeId, nodeIdList)
                .orderByDesc(ExecutionNodePropsEntity::getId);

        List<ExecutionNodePropsEntity> propsEntityList = list(queryWrapper);
        Map<Long, T> resultMap = new HashMap<>();
        if (!CollectionUtils.isEmpty(propsEntityList)) {
            for (ExecutionNodePropsEntity nodePropsEntity : propsEntityList) {
                String propsContent = nodePropsEntity.getPropsContent();
                if (StringUtils.isBlank(propsContent)) {
                    continue;
                }
                Long nodeId = nodePropsEntity.getNodeId();
                resultMap.computeIfAbsent(nodeId, key -> JSONUtil.toBean(propsContent, clazz));
            }
        }

        List<T> resultList = new ArrayList<>();
        for (Long nodeId : nodeIdList) {
            T result = resultMap.getOrDefault(nodeId, null);
            resultList.add(result);
        }
        return resultList;
    }
}
