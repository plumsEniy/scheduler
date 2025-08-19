package com.bilibili.cluster.scheduler.common.dto.bmr.config.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * @description:
 * @Date: 2024/2/19 17:59
 * @Author: nizhiqiang
 */
@Data
@NoArgsConstructor
public class ConfigItemInfo {
    long id;
    long configFileId;
    String creator;
    String updater;
    boolean deleted;
    LocalDateTime ctime;
    LocalDateTime mtime;

    private String itemKey;
    private String itemValue;
}
