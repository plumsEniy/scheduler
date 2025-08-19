package com.bilibili.cluster.scheduler.common.dto.bmr.metadata.resp;

import com.bilibili.cluster.scheduler.common.response.BaseMsgResp;
import lombok.Data;

import java.util.List;

@Data
public class QueryWechatRobotMsgResp extends BaseMsgResp {

    private List<String> obj;

}
