package com.bilibili.cluster.scheduler.common.dto.user;

import cn.hutool.core.annotation.Alias;
import lombok.Data;

/**
 * @description: department信息
 * @Date: 2024/3/13 19:45
 * @Author: nizhiqiang
 */
@Data
public class DepartmentInfo {
    private Long id;
    private String name;

    @Alias("workwx_id")
    private long workWxId;

    @Alias("workwx_parent_id")
    private long workWxParentId;

    private long ctime;

    private long mtime;
}
