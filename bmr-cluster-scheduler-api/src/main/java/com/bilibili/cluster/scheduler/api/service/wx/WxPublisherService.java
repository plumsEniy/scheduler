package com.bilibili.cluster.scheduler.api.service.wx;

import com.bilibili.cluster.scheduler.common.enums.wx.ProcessStatusEnum;

import java.util.List;

/**
 * @description: 微信通知
 * @Date: 2024/3/18 18:09
 * @Author: nizhiqiang
 */
public interface WxPublisherService {

    /**
     * 开始变更（变更通知群）
     *
     * @param changeLog
     * @return
     */
    public Long notifyChangeStart(String changeLog, String username);

    /**
     * 变更通过（变更通知群）
     *
     * @param notifyId
     */
    public void changeNotifyStatus(Long notifyId, String username, ProcessStatusEnum status);

    /**
     * 企业微信机器人通知
     * @param key
     * @param context   通知文本
     * @param userIdList
     */
    void wxRobotNotify(String key, String context);

    /**
     * 微信通知
     * @param operatorList
     * @param msgTypeText
     * @param message
     */
    void wxPushMsg(List<String> operatorList, String msgTypeText, String message);
}
