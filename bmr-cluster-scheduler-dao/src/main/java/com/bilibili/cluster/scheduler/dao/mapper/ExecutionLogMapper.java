package com.bilibili.cluster.scheduler.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bilibili.cluster.scheduler.common.entity.ExecutionLogEntity;
import com.bilibili.cluster.scheduler.common.enums.flowLog.LogTypeEnum;
import org.apache.ibatis.annotations.Param;

/**
 * <p>
 * Mapper 接口
 * </p>
 *
 * @author 谢谢谅解
 * @since 2024-01-19
 */
public interface ExecutionLogMapper extends BaseMapper<ExecutionLogEntity> {

    ExecutionLogEntity queryByExecuteIdAndLogType(@Param("executeId") Long executeId, @Param("logType") LogTypeEnum logType);

}
