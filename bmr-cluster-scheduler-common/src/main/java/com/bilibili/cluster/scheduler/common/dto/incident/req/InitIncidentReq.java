package com.bilibili.cluster.scheduler.common.dto.incident.req;

import cn.hutool.core.annotation.Alias;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @description: 变更初始化
 * @Date: 2024/4/22 16:16
 * @Author: nizhiqiang
 */

@Data
public class InitIncidentReq {

    @Alias("caster_extra_key")
    private String casterExtraKey;

    @Alias("behavior_id")
    private String behaviorId;

    private String env;

    private Long timestamp;

    @Alias("change_type")
    private Integer changeType;

    @Alias("cmdb_model_id")
    private String cmdbModelId;

    private String refer;

    private String desc;

    private String before = "{}";

    private List<AffectsDTO> affects;

    @Alias("order_id")
    private String orderId;

    private String title;

    private String after = "{}";

    @Alias("change_uuid")
    private String changeUuid;

    private String operator;

    @Alias("platform_id")
    private Integer platformId;

    @Alias("change_target")
    private String changeTarget;

    private List<String> zones;

    @NoArgsConstructor
    @Data
    public static class AffectsDTO {
        private Integer topology;

        @Alias("source_type")
        private Integer sourceType;

        @Alias("specific_source")
        private String specificSource;
    }
}
