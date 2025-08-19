package com.bilibili.cluster.scheduler.common.dto.bmr.metadata.resp;

import com.bilibili.cluster.scheduler.common.dto.bmr.metadata.MetadataPackageData;
import com.bilibili.cluster.scheduler.common.response.BaseMsgResp;
import lombok.Data;

/**
 * @description: 查询安装包
 * @Date: 2024/5/15 15:00
 * @Author: nizhiqiang
 */
@Data
public class QueryPackageMsgResp extends BaseMsgResp {
    private MetadataPackageData obj;

}
