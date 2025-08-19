package com.bilibili.cluster.scheduler.common.dto.bmr.config.model;

import lombok.Data;

@Data
public class FileDownloadData {
    private String fileName;
    private String downloadUrl;
    private String fileMd5;
}
