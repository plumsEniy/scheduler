package com.bilibili.cluster.scheduler.common.dto.user.req;

import lombok.Data;
import org.springframework.http.HttpMethod;

/**
 * @description: 查询用户信息请求
 * @Date: 2024/3/6 14:58
 * @Author: nizhiqiang
 */
@Data
public class QueryUserInfoReq {

    private String englishName;
}
