package com.bilibili.cluster.scheduler.common.utils;

import cn.hutool.http.Header;
import cn.hutool.http.HttpRequest;
import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

@Slf4j
public class RobotSendMessageUtils {
    public static final String ROBOT_URL = "https://qyapi.weixin.qq.com/cgi-bin/webhook/send?key=";
    public static final String ROBOT_PARAM_MSGTYPE = "msgtype";
    public static final String MESSAGE_TYPE_TEXT = "text";
    public static final String MESSAGE_TYPE_MARKDOWN = "markdown";
    public static final String ROBOT_PARAM_CONTENT = "content";


    public static void sendMessageByRobot(String robotKey, String content) {
        try {
            String url = ROBOT_URL + robotKey;
            log.info("URL: " + url);
            Map<String, Object> paramMap = new HashMap<>();
            Map<String, Object> contentMap = new HashMap<>();
            contentMap.put(ROBOT_PARAM_CONTENT, content);
            paramMap.put(ROBOT_PARAM_MSGTYPE, MESSAGE_TYPE_TEXT);
            paramMap.put(MESSAGE_TYPE_TEXT, contentMap);
            HttpRequest.post(url)
                    .header(Header.CONTENT_TYPE, "application/json")
                    .body(JSON.toJSONString(paramMap))
                    .timeout(30000)
                    .execute().body();
        } catch (Exception e) {
            log.error("robot send message error:", e);
            throw new RuntimeException("robot send message error:" + e.getMessage());
        }
    }
}
