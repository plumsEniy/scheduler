package com.bilibili.cluster.scheduler.common.dto.clickhouse.clickhouse.templates.configFile;

import com.bilibili.cluster.scheduler.common.dto.clickhouse.clickhouse.templates.PortInfo;
import lombok.Data;

import java.util.List;

/**
 * @description: 对应serviceTemplates.yaml文件
 * @Date: 2025/1/24 11:06
 * @Author: nizhiqiang
 */

@Data
public class FileServiceTemplate {
    List<PortInfo> ports;
}
