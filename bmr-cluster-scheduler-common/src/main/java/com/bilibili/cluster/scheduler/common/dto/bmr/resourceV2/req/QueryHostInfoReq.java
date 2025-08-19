package com.bilibili.cluster.scheduler.common.dto.bmr.resourceV2.req;

import com.bilibili.cluster.scheduler.common.dto.bmr.resourceV2.HostInitStateEnum;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.util.List;


@Data
public class QueryHostInfoReq {

    /**
     * 主机名列表
     */
    private List<String> hostNameList;

    /**
     * 主机初始化状态
     */
    private HostInitStateEnum initState;

    /**
     * 页码
     */
    @NotNull(message = "pageNum can not be null")
    private int pageNum ;

    /**
     * 每页条数
     */
    @NotNull(message = "pageSize can not be null")
    private int pageSize;

}
