package com.bilibili.cluster.scheduler.common.dto.caster.req;

import cn.hutool.core.annotation.Alias;
import lombok.Data;

import java.util.List;

/**
 * @description: 删除pvc
 * @Date: 2025/4/1 19:21
 * @Author: nizhiqiang
 */
@Data
public class DeletePvcReq {

    @Alias("cluster_id")
//    集群id
    private long clusterId;
    private String namespace;
    @Alias("pvc_names")
//    pod名
    private List<String> pvcNames;
}
