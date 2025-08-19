package com.bilibili.cluster.scheduler.common.dto.spark.manager.resp;

import com.bilibili.cluster.scheduler.common.dto.bmr.metadata.InstallationPackage;
import com.bilibili.cluster.scheduler.common.response.BaseMsgResp;
import lombok.Data;

@Data
public class QueryFallbackPackageResp extends BaseMsgResp {

    InstallationPackage obj;

}
