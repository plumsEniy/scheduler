package com.bilibili.cluster.scheduler.common.dto.oa;

import lombok.Data;

/**
 * @description: submit参数
 * @Date: 2024/3/6 15:46
 * @Author: nizhiqiang
 */
@Data
public class SubmitArgs<T> {

    private ActionInfo actionInfo;
    private BasicInfo basicInfo;
    private T form;
    private TaskInfo taskInfo;


}
