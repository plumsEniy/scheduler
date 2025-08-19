package com.bilibili.cluster.scheduler.common.dto.oa.manager.flink;

import cn.hutool.core.annotation.Alias;
import com.bilibili.cluster.scheduler.common.dto.oa.FormData;
import com.bilibili.cluster.scheduler.common.dto.oa.OAUserContent;
import lombok.Data;

import java.util.LinkedList;
import java.util.List;

@Data
public class FlinkManagerReqForm {

    @Alias(value = "textarea_h37tkl1o")
    private String remark;

    @Alias(value = "staffpicker_kl8munu8")
    private LinkedList<OAUserContent> submitUserContents;

    @Alias(value = "staffpicker_8p5ksfng")
    private LinkedList<OAUserContent> approverContents;

}
