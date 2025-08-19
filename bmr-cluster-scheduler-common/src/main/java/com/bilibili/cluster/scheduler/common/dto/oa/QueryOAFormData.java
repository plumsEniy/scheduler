package com.bilibili.cluster.scheduler.common.dto.oa;

import lombok.Data;

import java.util.List;

/**
 * @description: 查询oa的数据
 * @Date: 2024/3/6 11:07
 * @Author: nizhiqiang
 */
@Data
public class QueryOAFormData<T> {

    private ActionInfo actionInfo;
    private BasicInfo basicInfo;
    private T formData;
    private FormInfo formInfo;
    private TaskInfo taskInfo;


    @Data
    public static class FormInfo {
        private List<String> button;
    }
}
