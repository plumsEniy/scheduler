package com.bilibili.cluster.scheduler.common.dto.bmr.metadata.resp;


import com.bilibili.cluster.scheduler.common.dto.bmr.metadata.MetadataMonitorConf;
import com.bilibili.cluster.scheduler.common.response.BaseMsgResp;
import lombok.Data;

import java.util.List;

@Data
public class QueryMonitorConfResp extends BaseMsgResp {

    private List<MetadataMonitorConf> obj;

}
