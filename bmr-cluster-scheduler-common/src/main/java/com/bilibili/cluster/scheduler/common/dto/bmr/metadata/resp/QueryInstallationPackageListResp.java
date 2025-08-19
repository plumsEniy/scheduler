package com.bilibili.cluster.scheduler.common.dto.bmr.metadata.resp;

import com.bilibili.cluster.scheduler.common.dto.bmr.metadata.BasePage;
import com.bilibili.cluster.scheduler.common.dto.bmr.metadata.InstallationPackage;
import com.bilibili.cluster.scheduler.common.response.BaseMsgResp;
import lombok.Data;

/**
 * @description:
 * @Date: 2025/4/28 17:07
 * @Author: nizhiqiang
 */

@Data
public class QueryInstallationPackageListResp extends BaseMsgResp {
    BasePage<InstallationPackage> obj;
}
