package com.bilibili.cluster.scheduler.api.model;

import lombok.Data;

@Data
public class DashboardLoginUser {

    private Integer code;
    private String message;
    private String sessionId;
    private String username;
    private String caller;
}
