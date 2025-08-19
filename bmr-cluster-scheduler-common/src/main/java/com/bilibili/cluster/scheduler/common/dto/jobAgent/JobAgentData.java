package com.bilibili.cluster.scheduler.common.dto.jobAgent;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class JobAgentData {

    @JsonProperty(value = "not_install")
    private List<String> notInstall;

    @JsonProperty(value = "low_version")
    private List<String> lowVersion;

    @JsonProperty(value = "warn_info")
    private List<String> warnInfo;
}
