package com.bilibili.cluster.scheduler.common.dto.bmr.config.model;

import com.bilibili.cluster.scheduler.common.enums.bmr.config.FileOperateType;
import com.bilibili.cluster.scheduler.common.enums.bmr.config.FileType;
import lombok.Data;

import java.util.LinkedHashMap;

@Data
public class FileDTO {

    private String fileName;
    /**
     * 文件操作类型
     */
    private FileOperateType fileOperationEnum;
    /**
     * 文件类型  map 还是 文本类型
     */
    private FileType fileTypeEnum;

    /**
     * 文件是否解析,0:false,1:true
     */
    private Boolean analysis;

    private String fileContext;

    private LinkedHashMap<String ,String> fileContextMap;



}
