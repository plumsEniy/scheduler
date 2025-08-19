package com.bilibili.cluster.scheduler.common.dto.scheduler.model;

import lombok.Data;

/**
 * @description: task解析
 * @Date: 2024/5/16 07:13
 * @Author: nizhiqiang
 */

@Data
public class SchedTaskDefine {
    private String taskCode;
    private String name;
    private String rawScript;
    private String preTaskCode;

    public SchedTaskDefine() {

    }

    public SchedTaskDefine(DagData dagData, ProcessTaskRelation relationList) {
        this.taskCode = String.valueOf(relationList.getPostTaskCode());
        this.preTaskCode = String.valueOf(relationList.getPreTaskCode());
        for (TaskDefinition tdl : dagData.getTaskDefinitionList()) {
            if (tdl.getCode().equals(this.taskCode)) {
                this.name = tdl.getName();
                this.rawScript = tdl.getTaskParams().getRawScript();
            }
        }
    }
}
