package com.bilibili.cluster.scheduler.common.dto.oa.manager;

import cn.hutool.core.annotation.Alias;
import com.bilibili.cluster.scheduler.common.Constants;
import com.bilibili.cluster.scheduler.common.dto.oa.OAUserContent;
import lombok.Data;

import java.util.List;

@Data
public class UnifiedReqForm {

    @Alias(value = "staffpicker_1posd5lo")
    private List<OAUserContent> submitUserContents;

    @Alias(value = "staffpicker_q6m9b6o")
    private List<OAUserContent> approverContents;

    @Alias(value = "staffpicker_teefkdg")
    private List<OAUserContent> carbonCopyContents;

    @Alias(value = "code_editor_ov3shuog")
    private CodeDiff codeDiff;

    @Alias(value = "input_iu1d3jf")
    private String changeComponent;

    @Alias(value = "input_e17o71so")
    private String env;

    @Alias(value = "input_7l2hm28g")
    private String changeType;

    @Alias(value = "textarea_2h74j1f8")
    private String remark;

    /**
     * 是否需要字段对比，默认为false，如果为true会显示codedifff（即字段对比）
     */
    @Alias(value = "input_rod3rp1")
    private String useCodeDiff = Constants.FALSE;

    private String block;

}
