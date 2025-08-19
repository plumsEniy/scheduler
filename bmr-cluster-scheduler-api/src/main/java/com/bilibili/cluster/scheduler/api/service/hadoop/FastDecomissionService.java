package com.bilibili.cluster.scheduler.api.service.hadoop;

import com.bilibili.cluster.scheduler.common.dto.hadoop.FastDecommissionTaskDTO;
import com.bilibili.cluster.scheduler.common.enums.dataNode.DataNodeVersionEnum;

import java.util.List;

/**
 * @description: datanode的fastdecommission服务
 * @Date: 2024/5/11 16:26
 * @Author: nizhiqiang
 */
public interface FastDecomissionService {

    /**
     * 创建fastdecmission
     *
     * @param dataNodeList
     * @return
     */
    Long createFastDecommission(List<String> dataNodeList);

    /**
     * 启动fastdecmission
     *
     * @param dnId
     * @param dataNodeVersion
     */
    void startFastDecommission(Long dnId, DataNodeVersionEnum dataNodeVersion);

    /**
     * 查询fast decmission进度
     *
     * @param dnId
     * @return
     */
    List<FastDecommissionTaskDTO> queryFastDecommission(Long dnId);

}
