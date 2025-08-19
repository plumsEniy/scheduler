package com.bilibili.cluster.scheduler.common.dto.hbo.model;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @description:
 * @Date: 2024/12/26 14:21
 * @Author: nizhiqiang
 */
@NoArgsConstructor
@Data
public class HboJobInfo extends HboJob {
    private Long id;

    private Boolean isDeleted;

    private Long ctime;

    private Long mtime;
}
