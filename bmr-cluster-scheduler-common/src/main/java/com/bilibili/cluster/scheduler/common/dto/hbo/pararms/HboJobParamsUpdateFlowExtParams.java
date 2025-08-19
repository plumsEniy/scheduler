package com.bilibili.cluster.scheduler.common.dto.hbo.pararms;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
 * @description:
 * @Date: 2024/12/30 11:27
 * @Author: nizhiqiang
 */

@Data
public class HboJobParamsUpdateFlowExtParams {

    Map<String, String> addParamsMap = new HashMap<>();

    Map<String, String> removeParamsMap = new HashMap<>();
}
