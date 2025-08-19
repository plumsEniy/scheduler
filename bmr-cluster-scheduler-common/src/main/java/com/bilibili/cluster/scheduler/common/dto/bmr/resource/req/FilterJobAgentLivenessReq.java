package com.bilibili.cluster.scheduler.common.dto.bmr.resource.req;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

/**
 * @description: 过滤jobagent丢失的主机
 * @Date: 2024/5/16 11:15
 * @Author: nizhiqiang
 */

@Data
@AllArgsConstructor
public class FilterJobAgentLivenessReq {
    List<String> hostNameList;

}
