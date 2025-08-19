package com.bilibili.cluster.scheduler.common.dto.bmr.config.model;

import com.bilibili.cluster.scheduler.common.enums.bmr.config.FileOperateType;
import com.bilibili.cluster.scheduler.common.enums.bmr.config.FileType;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * @description: 更新文件的dto
 * @Date: 2024/4/24 16:47
 * @Author: nizhiqiang
 */
@NoArgsConstructor
@Data
public class UpdateFileDto {

    private Long componentId;
    private String content;
    private String fileName;
    /**
     * 根据file type传参数
     * List:修改的参数往iplist里填写
     * map:修改的参数往map里填
     */
    private FileType fileType;
    private List<String> ipList;
    private Map<String, String> map;
    private FileOperateType operateType;


}
