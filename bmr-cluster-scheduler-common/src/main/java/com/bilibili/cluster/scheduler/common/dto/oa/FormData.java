package com.bilibili.cluster.scheduler.common.dto.oa;

import lombok.Data;

import java.util.List;

/**
 * @description: forma
 * @Date: 2024/3/6 15:55
 * @Author: nizhiqiang
 */
public interface FormData {

    List<SubmitInfo> getSubmitInfo();

    String getCreator();

    String getTextarea();

    List<FormData.ApproverInfo> getApproverInfoList();


    @Data
    class SubmitInfo {
        private String name;
        private String membertype;
        private String title;
        private String key;
    }

    @Data
    class ApproverInfo {
        private String name;
        private String membertype;
        private String title;
        private String key;
    }
}
