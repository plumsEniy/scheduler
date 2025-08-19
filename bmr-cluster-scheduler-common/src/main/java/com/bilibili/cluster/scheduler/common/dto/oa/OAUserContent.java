package com.bilibili.cluster.scheduler.common.dto.oa;

import cn.hutool.core.annotation.Alias;
import com.bilibili.cluster.scheduler.common.dto.user.UserInfo;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @description: 提交用户内容
 * @Date: 2024/3/6 14:44
 * @Author: nizhiqiang
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class OAUserContent {

    private String path;

    private String name;

    @Alias(value = "membertype")
    private String memberType = "1";

    private String title;

    private String key;

    public OAUserContent(UserInfo userInfo) {
        this.key = userInfo.getEnglishName();
        this.name = userInfo.getName();
        this.title = userInfo.getName();
    }
}
