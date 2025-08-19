package com.bilibili.cluster.scheduler.common.dto.bmr.metadata.resp;

import com.bilibili.cluster.scheduler.common.response.BaseMsgResp;
import lombok.Data;

/**
 * @description: 查询默认安装包版本
 * @Date: 2024/10/22 11:02
 * @Author: nizhiqiang
 */

@Data
public class QueryDefaultPackageResp extends BaseMsgResp {
    long obj;
}
