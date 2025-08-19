package com.bilibili.cluster.scheduler.common.dto.bmr.config.req;

import com.bilibili.cluster.scheduler.common.dto.bmr.config.model.FileDTO;
import lombok.Data;

import java.util.List;

@Data
public class SaveFileReq {

    private Long componentId;


    private Long configGroupId;

    private String versionDesc;

    private List<FileDTO> fileDTOS;

    private String type;

}
