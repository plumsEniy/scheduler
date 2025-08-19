package com.bilibili.cluster.scheduler.common.dto.caster.resp;

import lombok.Data;

@Data
public class AuthPlatformResp extends BaseComResp {

    AuthPlatformData data;

    @Data
    public static class AuthPlatformData{
        String token;
        String platform_id;
        String user_name;
        String secret;
        Integer expired;
        Boolean admin;
    }
}
