package com.bilibili.cluster.scheduler.common.dto.presto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @description: preto对象
 * @Date: 2024/6/4 15:14
 * @Author: nizhiqiang
 */
@Data
@NoArgsConstructor
public class PrestoYamlObj {
    private List<PrestoCatalog> catalogList;
    private String imageName;
    /**
     * 启动脚本
     */
    private String start;
    private String capacity;
    private PrestoConfig coordinator;
    private PrestoConfig resource;
    private PrestoConfig worker;
    private List<PrestoAdditionalProps> additionalPrestoPropsList;

}
