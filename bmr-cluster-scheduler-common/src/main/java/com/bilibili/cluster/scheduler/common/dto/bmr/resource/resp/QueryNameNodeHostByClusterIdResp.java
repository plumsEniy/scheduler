package com.bilibili.cluster.scheduler.common.dto.bmr.resource.resp;

import com.bilibili.cluster.scheduler.common.dto.bmr.resource.ComponentNodeDetail;
import com.bilibili.cluster.scheduler.common.response.BaseMsgResp;
import lombok.Data;

import java.util.List;

/**
 * @description: 查询namenode节点
 * @Date: 2024/5/13 15:00
 * @Author: nizhiqiang
 */

@Data
public class QueryNameNodeHostByClusterIdResp extends BaseMsgResp {

    private List<ComponentNodeDetail> obj;
}
