package com.bilibili.cluster.scheduler.common.dto.tide.req;

import com.bilibili.cluster.scheduler.common.dto.tide.type.DynamicScalingStrategy;
import com.bilibili.cluster.scheduler.common.dto.tide.type.ScalingConfigType;
import com.bilibili.cluster.scheduler.common.enums.resourceV2.TideClusterType;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.util.Objects;

@Data
public class DynamicScalingQueryListPageReq {

    @NotNull
    @Min(value = 1, message = "pageNum 最小值为 1")
    @ApiModelProperty(value = "页码", example = "1", required = true)
    private Integer pageNum = 1;

    @NotNull
    @Min(value = 1, message = "pageSize 最小值为 1")
    @ApiModelProperty(value = "每页大小", example = "10", required = true)
    private Integer pageSize = 10;

    @ApiModelProperty(value = "集群ID", example = "1")
    private Long clusterId;

    @ApiModelProperty(value = "动态伸缩策略", example = "FIRST_EXPAND_THEN_SHRINK", required = true)
    private DynamicScalingStrategy dynamicScalingStrategy;

    @ApiModelProperty(value = "配置归属类型", example = "PRESTO")
    private TideClusterType configBelong;

    /**
     * 默认请求
     */
    public static DynamicScalingQueryListPageReq defaultReq(TideClusterType configBelong) {
        DynamicScalingQueryListPageReq tidalTaskQueryListPageReq = new DynamicScalingQueryListPageReq();
        tidalTaskQueryListPageReq.setPageNum(1);
        tidalTaskQueryListPageReq.setPageSize(9999);
        if (!Objects.isNull(configBelong)) {
            tidalTaskQueryListPageReq.setConfigBelong(configBelong);
        }
        return tidalTaskQueryListPageReq;
    }

}
