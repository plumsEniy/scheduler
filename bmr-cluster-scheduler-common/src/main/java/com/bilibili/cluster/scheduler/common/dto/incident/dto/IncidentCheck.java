package com.bilibili.cluster.scheduler.common.dto.incident.dto;

import cn.hutool.core.annotation.Alias;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @description: 变更检查
 * @Date: 2024/4/22 18:02
 * @Author: nizhiqiang
 */
@NoArgsConstructor
@Data
public class IncidentCheck {
    @Alias("scene_info")
    private SceneInfoDTO sceneInfo;
    
    @Alias("navigation_result")
    private List<NavigationResultDTO> navigationResult;
    @Alias("action")
    private Integer action;
    @Alias("control_state")
    private ControlStateDTO controlState;
    @Alias("result")
    private Integer result;
    @Alias("change_uuid")
    private String changeUuid;

    @NoArgsConstructor
    @Data
    public static class SceneInfoDTO {
        @Alias("access_navigation")
        private List<AccessNavigationDTO> accessNavigation;
        @Alias("type")
        private String type;
        @Alias("change_info_schema")
        private String changeInfoSchema;
        @Alias("post_navigation")
        private List<PostNavigationDTO> postNavigation;
        @Alias("name")
        private String name;
        @Alias("finish_navigation")
        private List<FinishNavigationDTO> finishNavigation;
        @Alias("pre_navigation")
        private List<PreNavigationDTO> preNavigation;
        @Alias("uid")
        private String uid;
        @Alias("desc")
        private String desc;
        @Alias("resource_type")
        private String resourceType;
        @Alias("step_info_schema")
        private String stepInfoSchema;

        @NoArgsConstructor
        @Data
        public static class AccessNavigationDTO {
            @Alias("name")
            private String name;
            @Alias("specific_spec")
            private String specificSpec;
            @Alias("config_refer")
            private String configRefer;
            @Alias("arg_schema")
            private String argSchema;
            @Alias("config_uid")
            private String configUid;
            @Alias("desc")
            private String desc;
            @Alias("creator")
            private String creator;
            @Alias("status")
            private Integer status;
            @Alias("uid")
            private String uid;
            @Alias("handle_action")
            private Integer handleAction;
            @Alias("spec")
            private String spec;
            @Alias("failed_action")
            private Integer failedAction;
            @Alias("check_mode")
            private Integer checkMode;
            @Alias("type")
            private Integer type;
            @Alias("order")
            private Integer order;
        }

        @NoArgsConstructor
        @Data
        public static class PostNavigationDTO {
            @Alias("spec")
            private String spec;
            @Alias("desc")
            private String desc;
            @Alias("uid")
            private String uid;
            @Alias("failed_action")
            private Integer failedAction;
            @Alias("arg_schema")
            private String argSchema;
            @Alias("order")
            private Integer order;
            @Alias("handle_action")
            private Integer handleAction;
            @Alias("check_mode")
            private Integer checkMode;
            @Alias("type")
            private Integer type;
            @Alias("specific_spec")
            private String specificSpec;
            @Alias("config_uid")
            private String configUid;
            @Alias("config_refer")
            private String configRefer;
            @Alias("status")
            private Integer status;
            @Alias("name")
            private String name;
            @Alias("creator")
            private String creator;
        }

        @NoArgsConstructor
        @Data
        public static class FinishNavigationDTO {
            @Alias("handle_action")
            private Integer handleAction;
            @Alias("check_mode")
            private Integer checkMode;
            @Alias("desc")
            private String desc;
            @Alias("type")
            private Integer type;
            @Alias("status")
            private Integer status;
            @Alias("spec")
            private String spec;
            @Alias("name")
            private String name;
            @Alias("order")
            private Integer order;
            @Alias("uid")
            private String uid;
            @Alias("config_uid")
            private String configUid;
            @Alias("creator")
            private String creator;
            @Alias("failed_action")
            private Integer failedAction;
            @Alias("specific_spec")
            private String specificSpec;
            @Alias("arg_schema")
            private String argSchema;
            @Alias("config_refer")
            private String configRefer;
        }

        @NoArgsConstructor
        @Data
        public static class PreNavigationDTO {
            @Alias("arg_schema")
            private String argSchema;
            @Alias("specific_spec")
            private String specificSpec;
            @Alias("config_refer")
            private String configRefer;
            @Alias("spec")
            private String spec;
            @Alias("order")
            private Integer order;
            @Alias("creator")
            private String creator;
            @Alias("uid")
            private String uid;
            @Alias("status")
            private Integer status;
            @Alias("failed_action")
            private Integer failedAction;
            @Alias("config_uid")
            private String configUid;
            @Alias("type")
            private Integer type;
            @Alias("desc")
            private String desc;
            @Alias("name")
            private String name;
            @Alias("handle_action")
            private Integer handleAction;
            @Alias("check_mode")
            private Integer checkMode;
        }
    }

    @NoArgsConstructor
    @Data
    public static class ControlStateDTO {
        @Alias("control_whitelist")
        private Boolean controlWhitelist;
        @Alias("fastpass")
        private Boolean fastpass;
    }

    @NoArgsConstructor
    @Data
    public static class NavigationResultDTO {
        @Alias("action")
        private Integer action;
        @Alias("end_time")
        private Integer endTime;
        @Alias("result")
        private Integer result;
        @Alias("navigation")
        private NavigationDTO navigation;
        @Alias("refer")
        private String refer;
        @Alias("start_time")
        private Integer startTime;
        @Alias("fail_reason")
        private String failReason;

        @NoArgsConstructor
        @Data
        public static class NavigationDTO {
            @Alias("name")
            private String name;
            @Alias("order")
            private Integer order;
            @Alias("config_uid")
            private String configUid;
            @Alias("specific_spec")
            private String specificSpec;
            @Alias("handle_action")
            private Integer handleAction;
            @Alias("uid")
            private String uid;
            @Alias("status")
            private Integer status;
            @Alias("creator")
            private String creator;
            @Alias("arg_schema")
            private String argSchema;
            @Alias("desc")
            private String desc;
            @Alias("type")
            private Integer type;
            @Alias("spec")
            private String spec;
            @Alias("check_mode")
            private Integer checkMode;
            @Alias("config_refer")
            private String configRefer;
            @Alias("failed_action")
            private Integer failedAction;
        }
    }
}
