package com.bilibili.cluster.scheduler.common.dto.bmr.resource.resp;

import com.bilibili.cluster.scheduler.common.response.BaseMsgResp;
import lombok.Data;

import java.util.ArrayList;

/**
 * @description:
 * @Date: 2024/5/16 11:17
 * @Author: nizhiqiang
 */

@Data
public class FilterJobAgentLivenessResp extends BaseMsgResp {
    private ArrayList<String> obj;

}
