package com.bilibili.cluster.scheduler.common.dto.bmr.resource.req;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class QueryComponentNodeListReq {

    private Long clusterId;
    private Long componentId;
    private List<String> hostNameList;
    private Integer pageNum = 1;
    private Long pageSize = 20000L;
    private String applicationState = "RUNNING";

    /**
     * 是否需要DNS，true会查询dns接口
     */
    private boolean needDns = false;

    public QueryComponentNodeListReq(Long clusterId, Long componentId) {
        this.clusterId = clusterId;
        this.componentId = componentId;
    }

    public QueryComponentNodeListReq(Long clusterId, Long componentId, String applicationState) {
        this.clusterId = clusterId;
        this.componentId = componentId;
        this.applicationState = applicationState;
    }

    public QueryComponentNodeListReq(Long clusterId) {
        this.clusterId = clusterId;
    }


}
