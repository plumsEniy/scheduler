package com.bilibili.cluster.scheduler.common.dto.bmr.resource.req;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpMethod;

/**
 * @description: 通过集群id查询namenode节点
 * @Date: 2024/5/13 14:56
 * @Author: nizhiqiang
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class QueryNameNodeHostByClusterIdReq {

    private Long clusterId;
}
