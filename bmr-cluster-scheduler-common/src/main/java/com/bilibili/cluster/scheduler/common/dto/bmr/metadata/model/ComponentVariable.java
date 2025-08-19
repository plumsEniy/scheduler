package com.bilibili.cluster.scheduler.common.dto.bmr.metadata.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @description:
 * @Date: 2024/5/15 15:19
 * @Author: nizhiqiang
 */
@NoArgsConstructor
@Data
public class ComponentVariable {
    @JsonProperty("ctime")
    private String ctime;
    @JsonProperty("mtime")
    private String mtime;
    @JsonProperty("creator")
    private String creator;
    @JsonProperty("updater")
    private String updater;
    @JsonProperty("deleted")
    private Boolean deleted;
    @JsonProperty("id")
    private Integer id;
    @JsonProperty("sort")
    private Integer sort;
    @JsonProperty("code")
    private String code;
    @JsonProperty("remark")
    private String remark;
    @JsonProperty("dictLabel")
    private String dictLabel;
    @JsonProperty("dictValue")
    private String dictValue;
}
