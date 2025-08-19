package com.bilibili.cluster.scheduler.common.dto.bmr.config.resp;

import com.bilibili.cluster.scheduler.common.dto.bmr.config.model.FileDownloadData;
import com.bilibili.cluster.scheduler.common.response.BaseMsgResp;
import lombok.Data;

@Data
public class QueryDownloadInfoByComponentIdResp extends BaseMsgResp {
    private FileDownloadData obj;

}
