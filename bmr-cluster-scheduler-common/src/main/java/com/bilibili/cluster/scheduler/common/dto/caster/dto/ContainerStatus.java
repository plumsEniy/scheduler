package com.bilibili.cluster.scheduler.common.dto.caster.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @description: 容器状态
 * @Date: 2024/8/22 10:56
 * @Author: nizhiqiang
 */

@NoArgsConstructor
@Data
public class ContainerStatus {

    @JsonProperty("name")
    private String name;
    @JsonProperty("state")
    private StateDTO state;
    @JsonProperty("ready")
    private Boolean ready;
    @JsonProperty("restartCount")
    private Integer restartCount;
    @JsonProperty("image")
    private String image;
    @JsonProperty("imageID")
    private String imageID;
    @JsonProperty("containerID")
    private String containerID;
    @JsonProperty("started")
    private Boolean started;

    @NoArgsConstructor
    @Data
    public static class StateDTO {
        @JsonProperty("running")
        private RunningDTO running;

        @NoArgsConstructor
        @Data
        public static class RunningDTO {
            @JsonProperty("startedAt")
            private String startedAt;
        }
    }
}
