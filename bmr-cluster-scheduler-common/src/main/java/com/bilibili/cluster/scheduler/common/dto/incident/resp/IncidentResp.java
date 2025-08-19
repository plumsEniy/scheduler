package com.bilibili.cluster.scheduler.common.dto.incident.resp;

import com.bilibili.cluster.scheduler.common.dto.incident.dto.IncidentCheck;
import com.bilibili.cluster.scheduler.common.response.BaseResp;
import lombok.Data;

@Data
public class IncidentResp extends BaseResp {

    private IncidentCheck obj;
}
