package com.bilibili.cluster.scheduler.common.dto.parameters.dto.flow.datanode;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * @description:dn下线参数
 * @Date: 2024/5/14 11:53
 * @Author: nizhiqiang
 */

@Data
public class DnEvictionParameters {
    private Long dnId;

    private LocalDateTime dnStartTime;

    private boolean hasCheckFastDecomission = false;

}
