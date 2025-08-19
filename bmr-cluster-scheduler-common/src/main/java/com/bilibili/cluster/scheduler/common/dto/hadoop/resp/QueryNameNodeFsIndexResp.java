package com.bilibili.cluster.scheduler.common.dto.hadoop.resp;

import com.bilibili.cluster.scheduler.common.dto.hadoop.NameNodeFsIndex;
import lombok.Data;

import java.util.List;

/**
 * @description: 查询namenode参数的resp
 * @Date: 2024/5/13 17:04
 * @Author: nizhiqiang
 */
@Data
public class QueryNameNodeFsIndexResp {
    List<NameNodeFsIndex> beans;
}
