package com.bilibili.cluster.scheduler.common.dto.bmr.resourceV2.req;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.util.List;

@Data
public class QueryLogicGroupInfoReq {
    // 主键
    private Long id;
    // 集群ID
    @NotNull(message = "clusterId cannot be null")
    private Long clusterId;
    // 分组名称
    private String groupName;

    private String creator;

    private List<String> hostList;

    //模糊查询主机
    private String hostName;

    /**
     * 页码
     */
    private int pageNum = 1;
    /**
     * 每页条数
     */
    private int pageSize = 10;
}
