package com.bilibili.cluster.scheduler.common.dto.bmr.metadata.resp;

import com.bilibili.cluster.scheduler.common.response.BaseMsgResp;
import lombok.Data;

/**
 * @description: 查询下载信息
 * @Date: 2024/5/15 15:04
 * @Author: nizhiqiang
 */
@Data
public class QueryPackageDownloadResp extends BaseMsgResp {

    private String obj;

}
