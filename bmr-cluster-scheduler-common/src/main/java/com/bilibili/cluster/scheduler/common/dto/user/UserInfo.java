package com.bilibili.cluster.scheduler.common.dto.user;

import cn.hutool.core.annotation.Alias;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Date;

/**
 * @description: 用户信息
 * @Date: 2024/3/6 14:52
 * @Author: nizhiqiang
 */
@Data
public class UserInfo {
    private Long id;
    private String name;
    @Alias("user_id")
    private String userId;
    @Alias("workwx_main_department")
    private Long workwxMainDepartment;
    private String email;
    @Alias("english_name")
    private String englishName;
    private String telephone;
    private long ctime;
    private long mtime;
}
