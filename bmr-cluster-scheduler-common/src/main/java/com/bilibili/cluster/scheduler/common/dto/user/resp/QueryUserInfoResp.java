package com.bilibili.cluster.scheduler.common.dto.user.resp;

import com.bilibili.cluster.scheduler.common.response.BaseResp;
import com.bilibili.cluster.scheduler.common.dto.user.DepartmentInfo;
import com.bilibili.cluster.scheduler.common.dto.user.UserInfo;
import lombok.Data;

/**
 * @description: 查询用户信息
 * @Date: 2024/3/6 15:03
 * @Author: nizhiqiang
 */
@Data
public class QueryUserInfoResp extends BaseResp {
    private UserDepartmentInfo data;

    @Data
    public static class UserDepartmentInfo {
        private UserInfo user;
        private DepartmentInfo department;
    }

}
