package com.bilibili.cluster.scheduler.common.dto.hadoop.req;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

/**
 * @description: fast decomission
 * @Date: 2024/5/13 10:48
 * @Author: nizhiqiang
 */
@Data
@AllArgsConstructor
public class CreateFastDecommissionReq {
    List<String> dn;
}
