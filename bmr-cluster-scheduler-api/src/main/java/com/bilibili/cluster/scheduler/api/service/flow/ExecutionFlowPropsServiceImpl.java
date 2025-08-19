package com.bilibili.cluster.scheduler.api.service.flow;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bilibili.cluster.scheduler.api.bean.SpringApplicationContext;
import com.bilibili.cluster.scheduler.api.service.cache.CacheService;
import com.bilibili.cluster.scheduler.api.service.redis.RedisService;
import com.bilibili.cluster.scheduler.common.Constants;
import com.bilibili.cluster.scheduler.common.dto.button.DeployStageInfo;
import com.bilibili.cluster.scheduler.common.dto.button.StageStateEnum;
import com.bilibili.cluster.scheduler.common.dto.flow.ExecutionFlowProps;
import com.bilibili.cluster.scheduler.common.dto.flow.SaberUpdateProp;
import com.bilibili.cluster.scheduler.common.dto.flow.prop.BaseFlowExtPropDTO;
import com.bilibili.cluster.scheduler.common.dto.spark.params.SparkDeployFlowExtParams;
import com.bilibili.cluster.scheduler.common.entity.ExecutionFlowPropsEntity;
import com.bilibili.cluster.scheduler.common.utils.CacheUtils;
import com.bilibili.cluster.scheduler.common.utils.LocalDateFormatterUtils;
import com.bilibili.cluster.scheduler.dao.mapper.ExecutionFlowPropsMapper;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 谢谢谅解
 * @since 2024-01-26
 */
@Service
public class ExecutionFlowPropsServiceImpl extends ServiceImpl<ExecutionFlowPropsMapper, ExecutionFlowPropsEntity> implements ExecutionFlowPropsService {

    @Resource
    CacheService cacheService;

    @Resource
    RedisService redisService;

    @Override
    public <T> T getFlowPropByFlowId(Long flowId, Class<T> clazz) {
        LambdaQueryWrapper<ExecutionFlowPropsEntity> queryWrapper = new QueryWrapper<ExecutionFlowPropsEntity>().lambda();
        queryWrapper.eq(ExecutionFlowPropsEntity::getFlowId, flowId);
        ExecutionFlowPropsEntity executionFlowPropsEntity = getOne(queryWrapper);
        Optional<String> propContetOptional = Optional.ofNullable(executionFlowPropsEntity)
                .map(ExecutionFlowPropsEntity::getPropsContent);
        if (!propContetOptional.isPresent()) {
            return null;
        }
        return JSONUtil.toBean(propContetOptional.get(), clazz);
    }

    @Override
    public <T> T getFlowPropByFlowIdWithCache(Long flowId, Class<T> clazz) {
        String cacheKey = CacheUtils.getFlowPropsCacheKey(SpringApplicationContext.getEnv(), flowId);
        String cacheValue = redisService.get(cacheKey);
        T baseFlowProps;
        if (StringUtils.isBlank(cacheValue)) {
            baseFlowProps = getFlowPropByFlowId(flowId, clazz);
            redisService.set(cacheKey, JSONUtil.toJsonStr(baseFlowProps), Constants.ONE_MINUTES * 60);
            return baseFlowProps;
        }
        baseFlowProps = JSONUtil.toBean(cacheValue, clazz);
        return baseFlowProps;
    }

    @Override
    public void saveFlowProp(Long flowId, Object obj) {
        LambdaQueryWrapper<ExecutionFlowPropsEntity> queryWrapper = new QueryWrapper<ExecutionFlowPropsEntity>().lambda();
        queryWrapper.eq(ExecutionFlowPropsEntity::getFlowId, flowId);
        ExecutionFlowPropsEntity executionFlowPropsEntity = getOne(queryWrapper);
        String flowPropsJson = JSONUtil.toJsonStr(obj);
        if (executionFlowPropsEntity == null) {
            executionFlowPropsEntity = new ExecutionFlowPropsEntity();
            executionFlowPropsEntity.setFlowId(flowId);
            executionFlowPropsEntity.setPropsContent(flowPropsJson);
            save(executionFlowPropsEntity);
            return;
        }

        LambdaUpdateWrapper<ExecutionFlowPropsEntity> updateWrapper = new UpdateWrapper<ExecutionFlowPropsEntity>().lambda();
        updateWrapper.eq(ExecutionFlowPropsEntity::getFlowId, flowId)
                .set(ExecutionFlowPropsEntity::getPropsContent, flowPropsJson);
        boolean isSuccess = update(updateWrapper);

        if (isSuccess && obj instanceof BaseFlowExtPropDTO) {
            // 如果更新成功，刷新缓存
            BaseFlowExtPropDTO baseFlowExtPropDTO = (BaseFlowExtPropDTO) obj;
            String flowPropsKey = CacheUtils.getFlowPropsCacheKey(SpringApplicationContext.getEnv(), flowId);
            redisService.set(flowPropsKey, flowPropsJson, Constants.ONE_MINUTES * 60);

            String cacheKey = CacheUtils.getFlowExtParamsCacheKey(SpringApplicationContext.getEnv(), flowId);
            redisService.set(cacheKey, baseFlowExtPropDTO.getFlowExtParams(), Constants.ONE_MINUTES * 60);
        }
    }

    @Override
    public <T> T getFlowExtParamsByCache(Long flowId, Class<T> tClass) {
        String cacheKey = CacheUtils.getFlowExtParamsCacheKey(SpringApplicationContext.getEnv(), flowId);

        String cacheValue = redisService.get(cacheKey);
        if (StringUtils.isBlank(cacheValue)) {
            BaseFlowExtPropDTO flowProps = getFlowPropByFlowId(flowId, BaseFlowExtPropDTO.class);
            String flowExtParamsValue = flowProps.getFlowExtParams();
            cacheValue = flowExtParamsValue;
            redisService.set(cacheKey, cacheValue, Constants.ONE_MINUTES * 60);
        }
        T flowExtParams = JSONUtil.toBean(cacheValue, tClass);
        return flowExtParams;
    }

    @Override
    public boolean updateStageInfo(Long flowId, String execStage, StageStateEnum stageState, LocalDateTime startTime, LocalDateTime endTime, LocalDateTime allowedNextStageStartTime) {
        final BaseFlowExtPropDTO baseFlowExtPropDTO = getFlowPropByFlowId(flowId, BaseFlowExtPropDTO.class);
        if (Objects.isNull(baseFlowExtPropDTO)) {
            return false;
        }
        Map<Integer, DeployStageInfo> stageInfos = baseFlowExtPropDTO.getStageInfos();
        if (MapUtils.isEmpty(stageInfos)) {
            stageInfos = new LinkedHashMap<>();
            baseFlowExtPropDTO.setStageInfos(stageInfos);
        }
        int stageValue;
        try {
            stageValue = Integer.parseInt(execStage);
        } catch (Exception e) {
            log.error("parse exec stage error: execStage");
            return false;
        }

        DeployStageInfo stageInfo = stageInfos.computeIfAbsent(stageValue, stage -> new DeployStageInfo(stage));
        if (!Objects.isNull(stageState)) {
            stageInfo.setState(stageState);
        }
        if (!Objects.isNull(startTime)) {
            stageInfo.setStartTime(LocalDateFormatterUtils.format(Constants.FMT_DATE_TIME, startTime));
        }
        if (!Objects.isNull(endTime)) {
            stageInfo.setEndTime(LocalDateFormatterUtils.format(Constants.FMT_DATE_TIME, endTime));
        }
        if (!Objects.isNull(allowedNextStageStartTime)) {
            final String allowedNextStageStartTimeFormat = LocalDateFormatterUtils.format(Constants.FMT_DATE_TIME, allowedNextStageStartTime);
            stageInfo.setAllowedNextStageStartTime(allowedNextStageStartTimeFormat);
            baseFlowExtPropDTO.setAllowedNextProceedTime(allowedNextStageStartTimeFormat);
        }
        saveFlowProp(flowId, baseFlowExtPropDTO);
        return true;
    }
}
