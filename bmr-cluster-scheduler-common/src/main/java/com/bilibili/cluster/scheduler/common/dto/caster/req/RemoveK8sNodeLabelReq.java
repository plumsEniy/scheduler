package com.bilibili.cluster.scheduler.common.dto.caster.req;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.http.HttpMethod;

/**
 * @description: 移除k8s的标签
 * @Date: 2024/3/11 17:49
 * @Author: nizhiqiang
 */
@Data
@AllArgsConstructor
public class RemoveK8sNodeLabelReq {
    Integer cluster_id;
    String[] node_names;
    /**
     * label key
     */
    String key;
}
