package com.bilibili.cluster.scheduler.common.dto.user;

import lombok.Data;

import java.util.List;

/**
 * @description: 值班详情
 * @Date: 2024/3/19 17:34
 * @Author: nizhiqiang
 */

@Data
public class DutyDetail {
    private String desc;
    private String team;
    private String componentTitle;
    private String date;
    private String robotUrl;
    private List<DutyData> dutyData;

    @Data
    public static class DutyData{
        private String name;
        private String nickName;
        private String combineName;
        private String phone;
    }
}
