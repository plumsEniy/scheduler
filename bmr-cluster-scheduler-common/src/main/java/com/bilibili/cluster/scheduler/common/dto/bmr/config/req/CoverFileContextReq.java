package com.bilibili.cluster.scheduler.common.dto.bmr.config.req;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @description: 覆盖文件
 * @Date: 2025/2/13 15:25
 * @Author: nizhiqiang
 */

@AllArgsConstructor
@NoArgsConstructor
@Data
public class CoverFileContextReq {

    private long componentId;

    private String fileName;

    private String fileContext;
}
