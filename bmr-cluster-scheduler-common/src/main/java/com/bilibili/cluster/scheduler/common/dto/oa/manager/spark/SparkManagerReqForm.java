package com.bilibili.cluster.scheduler.common.dto.oa.manager.spark;

import cn.hutool.core.annotation.Alias;
import com.bilibili.cluster.scheduler.common.dto.oa.FormData;
import com.bilibili.cluster.scheduler.common.dto.oa.OAUserContent;
import lombok.Data;

import java.util.LinkedList;
import java.util.List;

@Data
public class SparkManagerReqForm {

    @Alias(value = "textarea_fkhp5nu8")
    private String remark;

    @Alias(value = "staffpicker_qebksg38")
    private LinkedList<OAUserContent> submitUserContents;

    @Alias(value = "staffpicker_50o8i9o8")
    private LinkedList<OAUserContent> approverContents;

}
