package com.bilibili.cluster.scheduler.common.dto.oa.manager.spark;

import cn.hutool.core.annotation.Alias;
import com.bilibili.cluster.scheduler.common.dto.oa.FormData;
import lombok.Data;

import java.util.List;

@Data
public class SparkManagerFormData implements FormData {

    @Alias(value = "staffpicker_kl8munu8")
    private List<FormData.SubmitInfo> submitInfo;
    private String creator;
    @Alias(value = "textarea_h37tkl1o")
    private String textarea;

    @Alias(value = "staffpicker_8p5ksfng")
    private List<FormData.ApproverInfo> approverInfoList;
}
