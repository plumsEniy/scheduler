package com.bilibili.cluster.scheduler.api.service.bmr.metadata;

import com.bilibili.cluster.scheduler.common.dto.bmr.metadata.MetadataClusterData;
import com.bilibili.cluster.scheduler.common.dto.bmr.metadata.MetadataComponentData;
import com.bilibili.cluster.scheduler.common.dto.bmr.metadata.MetadataMonitorConf;
import com.bilibili.cluster.scheduler.common.dto.bmr.metadata.MetadataPackageData;
import com.bilibili.cluster.scheduler.common.dto.bmr.metadata.InstallationPackage;
import com.bilibili.cluster.scheduler.common.dto.bmr.metadata.req.QueryInstallationPackageListReq;

import java.util.List;
import java.util.Map;

/**
 * @description: 元数据管理
 * @Date: 2024/5/14 16:29
 * @Author: nizhiqiang
 */
public interface BmrMetadataService {
    /**
     * 根据集群id查询组件信息
     * @param clusterId
     * @return
     */
    List<MetadataComponentData> queryComponentListByClusterId(long clusterId);

    /**
     * 查询组件信息
     * @param componentId
     * @return
     */
    MetadataComponentData queryComponentByComponentId(long componentId);

    /**
     * 查询安装包
     * @param packageId
     * @return
     */
    MetadataPackageData queryPackageDetailById(long packageId);

    /**
     * 查询安装包信息
     * @param packageId
     * @return
     */
    String queryPackageDownloadInfo(long packageId);

    /**
     * 查询变量
     * @param componentId
     * @return
     */
    Map<String, String> queryVariableByComponentId(long componentId);

    /**
     * 查询集群信息
     * @param clusterId
     * @return
     */
    MetadataClusterData queryClusterDetail(long clusterId);

    /**
     * 查询组件监控配置信息，用于节点上下线场景
     * @param clusterId
     * @param componentId
     */
    List<MetadataMonitorConf> queryMonitorConfList(Long clusterId, Long componentId);

    /**
     * 查询默认安装包
     * @param componentId
     * @return
     */
    long queryDefaultPackageIdByComponentId(Long componentId);


    // todo: api with spark manager, but now is metadata
    /**
     * 修改默认安装包
     * @param packageId
     * @param componentName
     */
    void updateDefaultVersion(Long packageId, String componentName);

    /**
     * 根据小版本查询安装包
     * @param minorVersion
     * @return
     */
    InstallationPackage queryPackageByMinorVersion(String minorVersion);

    /**
     * 移除默认安装包
     * @param componentName
     */
    void removeDefaultPackage(String componentName);

    /**
     * 查询spark默认安装包
     * @return          为null代表没有设置
     */
    InstallationPackage querySparkDefaultPackage();

    /**
     * 查询spark周边组件默认安装包
     * @return          为null代表没有设置
     */
    InstallationPackage querySparkPeripheryComponentDefaultPackage(String componentName);

    /**
     * 查询安装包列表
     * @return
     */
    List<InstallationPackage> queryInstallationPackageList(QueryInstallationPackageListReq req);

    /**
     * 根据组件id查询企微机器人
     * @param componentId
     * @return
     */
    List<String> queryWechatRobotByComponentId(long componentId);
}
