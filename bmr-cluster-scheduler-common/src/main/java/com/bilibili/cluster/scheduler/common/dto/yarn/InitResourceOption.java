package com.bilibili.cluster.scheduler.common.dto.yarn;

import lombok.Data;

@Data
public class InitResourceOption {

    InitResource resource;

    public InitResourceOption(){
        resource = new InitResource();
    }
}
