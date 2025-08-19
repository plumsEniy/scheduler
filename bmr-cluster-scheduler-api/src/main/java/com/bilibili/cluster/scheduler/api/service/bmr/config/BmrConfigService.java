package com.bilibili.cluster.scheduler.api.service.bmr.config;

import com.bilibili.cluster.scheduler.common.dto.bmr.config.ComponentConfigVersionEntity;
import com.bilibili.cluster.scheduler.common.dto.bmr.config.ConfigData;
import com.bilibili.cluster.scheduler.common.dto.bmr.config.ConfigDetailData;
import com.bilibili.cluster.scheduler.common.dto.bmr.config.model.*;
import com.bilibili.cluster.scheduler.common.dto.bmr.config.req.SaveFileReq;
import com.bilibili.cluster.scheduler.common.dto.bmr.config.req.UpdateSpecialKeyValueFileReq;
import com.bilibili.cluster.scheduler.common.enums.bmr.config.ConfigFileTypeEnum;
import com.bilibili.cluster.scheduler.common.enums.bmr.config.ConfigVersionType;
import com.bilibili.cluster.scheduler.common.enums.bmr.config.FileOperateType;
import com.bilibili.cluster.scheduler.common.enums.bmr.config.FileType;

import java.util.List;
import java.util.Map;

/**
 * @description: bmr配置服务
 * @Date: 2024/5/13 11:49
 * @Author: nizhiqiang
 */
public interface BmrConfigService {
    /**
     * 更新文件列表
     *
     * @param fileDtoList
     * @return
     */
    Boolean updateFileList(List<UpdateFileDto> fileDtoList);

    /**
     * 更新文件列表
     *
     * @param componentIdList
     * @param fileName
     * @param fileType        list参数则填在list，json参数则填在map
     * @param list
     * @param map
     * @param operateType
     * @return
     */
    Boolean updateFileList(List<Long> componentIdList, String fileName, FileType fileType
            , List<String> list, Map<String, String> map, FileOperateType operateType);

    /**
     * 查询下载信息
     *
     * @param componentId
     * @param fileName
     * @return
     */
    FileDownloadData queryDownloadInfoByComponentId(long componentId, String fileName);

    /**
     * 查询配置
     *
     * @param configId
     * @return
     */
    ConfigDetailData queryConfigDetailById(long configId);

    /**
     * 查询配置组信息
     *
     * @param configId
     * @return
     */
    List<ConfigGroupDo> queryConfigGroupInfoById(long configId);


    /**
     * 根据配置版本id查询item列表
     *
     * @param configVersionId   版本id和组件id二选一
     * @param componentId       版本id和组件id二选一
     * @param fileName
     * @param configFileType
     * @param configVersionType
     * @return
     */
    ConfigData queryItemListAndData(Long configVersionId, Long componentId, String fileName
            , ConfigFileTypeEnum configFileType, ConfigVersionType configVersionType);

    /**
     * 配置版本id
     *
     * @param configVersionId
     * @return
     */
    List<ConfigGroupRelationEntity> queryConfigGroupRelation(long configVersionId);

    List<ConfigFileEntity> queryFileListByGroupId(long configGroupId);

    /**
     * 需要根据组件id查询最新的配置id
     *
     * @param componentId
     * @return
     */
    ConfigDetailData queryConfigDetailByComponentId(long componentId);

    /**
     * 查询
     *
     * @param componentId
     * @param pageSize
     * @param pageNum
     * @return
     */
    List<ComponentConfigVersionEntity> queryComponentConfigVersionList(long componentId, String versionName, int pageSize, int pageNum);

    /**
     * 保存文件
     *
     * @param req
     */
    void saveFile(SaveFileReq req);

    /**
     * 更新特殊文件的key和value
     *
     * @param req
     */
    void updateSpecialKeyValueFile(UpdateSpecialKeyValueFileReq req);

    /**
     * 根据组件id查询默认配置包id
     *
     * @param componentId
     * @return
     */
    long queryDefaultConfigVersionIdByComponentId(long componentId);

    /**
     * 更新yarn-include文件信息
     *
     * @param componentId
     * @param fileName
     * @param operateType
     * @param ipList
     * @return
     */
    Boolean updateFileIpList(long componentId, String fileName, FileOperateType operateType, List<String> ipList);

    /**
     * 查询默认节点组
     *
     * @param configVersionId
     * @return
     */
    ConfigGroupRelationEntity queryDefaultGroupRelation(long configVersionId);

    /**
     * 全覆盖特殊文件文本，仅支持文本格式
     *
     * @param componentId
     * @param fileName
     * @param fileContext
     */
    void coverSpecialFileContext(long componentId, String fileName, String fileContext);
}
