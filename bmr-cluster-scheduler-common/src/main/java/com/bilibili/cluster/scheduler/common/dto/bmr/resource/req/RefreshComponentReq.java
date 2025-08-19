package com.bilibili.cluster.scheduler.common.dto.bmr.resource.req;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class RefreshComponentReq {

    private Long componentId;

    private String componentName;

    @ApiModelProperty("安装包节点磁盘版本")
    private String packageDiskVersion;

    @ApiModelProperty("配置包节点磁盘版本")
    private String configDiskVersion;

}
