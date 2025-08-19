package com.bilibili.cluster.scheduler.common.dto.bmr.metadata.resp;

import com.bilibili.cluster.scheduler.common.dto.bmr.metadata.InstallationPackage;
import com.bilibili.cluster.scheduler.common.response.BaseMsgResp;
import lombok.Data;

@Data
public class QueryInstallationPackageResp extends BaseMsgResp {

    InstallationPackage obj;

}
