package com.bilibili.cluster.scheduler.common.dto.yarn.req;

import com.bilibili.cluster.scheduler.common.dto.yarn.InitResourceOption;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AmiyaOffInitResourceReq {

    private InitResourceOption resourceOption;

    public AmiyaOffInitResourceReq(){
        resourceOption = new InitResourceOption();
    }

}
