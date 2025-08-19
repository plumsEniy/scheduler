package com.bilibili.cluster.scheduler.common.dto.bmr.config.req;

import com.bilibili.cluster.scheduler.common.enums.bmr.config.ConfigFileTypeEnum;
import com.bilibili.cluster.scheduler.common.enums.bmr.config.ConfigVersionType;
import lombok.Data;

/**
 * @description: 查询文件内容
 * @Date: 2024/6/7 14:57
 * @Author: nizhiqiang
 */

@Data
public class QueryConfigFileReq {

    /**
     * componentid和versionid必须传一个
     */
    private Long componentId;

    private Long configGroupId;

    private String fileName;

    private ConfigFileTypeEnum fileTypeEnum;

    /**
     * 需要查询的itemkey
     */
    private String key;

    private Long versionId;

    private ConfigVersionType versionType;

}