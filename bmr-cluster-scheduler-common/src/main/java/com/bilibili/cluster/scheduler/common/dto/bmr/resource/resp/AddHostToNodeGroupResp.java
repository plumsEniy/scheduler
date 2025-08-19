package com.bilibili.cluster.scheduler.common.dto.bmr.resource.resp;

import com.bilibili.cluster.scheduler.common.dto.bmr.resource.model.AddHostToNodeGroupData;
import com.bilibili.cluster.scheduler.common.response.BaseMsgResp;
import lombok.Data;

@Data
public class AddHostToNodeGroupResp extends BaseMsgResp {

    private AddHostToNodeGroupData obj;

}
