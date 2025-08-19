package com.bilibili.cluster.scheduler.common.dto.incident.req;

import cn.hutool.core.annotation.Alias;
import lombok.Data;

/**
 * @description: 结束变更
 * @Date: 2024/4/22 17:02
 * @Author: nizhiqiang
 */

@Data
public class UpdateIncidentStatusReq {

    @Alias("platform_id")
    private Integer platformId;

    private String env;

    private Long timestamp;

    /**
     * 7(已完成); 6(失败); 8(已回滚); 4(放弃)
     */
    private Integer status;

    @Alias("change_uuid")
    private String changeUuid;
}
