package com.bilibili.cluster.scheduler.api.service.user;

import com.bilibili.cluster.scheduler.common.dto.user.DutyDetail;
import com.bilibili.cluster.scheduler.common.dto.user.UserInfo;

import java.util.List;

/**
 * @description: 查询用户信息
 * @Date: 2024/3/6 14:51
 * @Author: nizhiqiang
 */
public interface UserInfoService {

    /**
     * 根据英语名查询用户信息
     *
     * @param englishName
     * @return
     */
    UserInfo getUserInfoByEnglishName(String englishName);

    /**
     * 根据英语名列表查询用户信息
     *
     * @param englishNameList
     * @return
     */
    List<UserInfo> getUserInfoByEnglishNameList(String... englishNameList);

    /**
     * 根据英语名列表查询用户信息
     *
     * @param englishNameList
     * @return
     */
    List<UserInfo> getUserInfoByEnglishNameList(List<String> englishNameList);

    /**
     * 查询值班详情
     * @param team
     * @param teamId
     * @return
     */
    DutyDetail queryDutyDetail(String team , Long teamId);

}
