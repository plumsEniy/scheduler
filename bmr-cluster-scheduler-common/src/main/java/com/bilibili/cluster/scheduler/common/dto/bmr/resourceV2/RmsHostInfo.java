package com.bilibili.cluster.scheduler.common.dto.bmr.resourceV2;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * @description: rms主机信息
 * @Date: 2025/7/23 17:11
 * @Author: nizhiqiang
 */

@NoArgsConstructor
@Data
public class RmsHostInfo  implements Serializable {
    private static final long serialVersionUID = 1L;

    private String bmcIp;

    private String bs;

    private String cpuAbstract;

    private String createAt;

    private GroupDTO group;

    private String hddAbstract;

    private MachinePackageDTO machinePackage;

    private String memAbstract;

    private String name;

    private String nvmeAbstract;

    private List<String> privateIPv4;

    private RackDTO rack;

    private String raidAbstract;

    private String sn;

    private String ssdAbstract;

    private String updateAt;

    private String updateAtGe;

    @JsonIgnore
    private Integer resourcePoolId;

    private IdcDTO idc;

    @NoArgsConstructor
    @Data
    public static class GroupDTO {
        @JsonProperty("name")
        private String name;
    }

    @NoArgsConstructor
    @Data
    public static class MachinePackageDTO {
        @JsonProperty("brandModel")
        private String brandModel;
        @JsonProperty("name")
        private String name;
    }

    @NoArgsConstructor
    @Data
    public static class RackDTO {
        @JsonProperty("name")
        private String name;
    }

    @Data
    public static class IdcDTO {
        @JsonProperty("name")
        private String name;

        @JsonProperty("zone")
        private ZoneDTO zone;
    }

    @Data
    public static class ZoneDTO {
        @JsonProperty("code")
        private String code;

        @JsonProperty("name")
        private String name;
    }

}