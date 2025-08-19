package com.bilibili.cluster.scheduler.common.dto.bmr.resource.req;


import lombok.Data;
import java.util.List;

@Data
public class RefreshNodeListReq {

    private Long clusterId;

    // 主机列表
    private List<String> hostList;

    // 发布操作类型
    private String deployTypeEnum;

    private List<RefreshComponentReq> refreshComponentReqList;

}
