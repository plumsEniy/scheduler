package com.bilibili.cluster.scheduler.api.tools;

import com.bilibili.cluster.scheduler.common.Constants;
import lombok.extern.slf4j.Slf4j;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

@Slf4j
public class RequestTools {

    public static String getSessionId(HttpServletRequest request, String sessionKey) {
        Cookie[] cookies = request.getCookies();
        String sessionId = null;

        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals(sessionKey)) {
                    sessionId = cookie.getValue();
                    if (!StringTools.isEmpty(sessionId))
                        return sessionId;
                }
            }
        }
        // 如果为空则从header中获取
        if (sessionId == null) {
            sessionId = request.getHeader(sessionKey);
        }
        // 如果为空则从param中获取，解决flash上传组件无法在某些浏览器提交cookies的问题
        if (sessionId == null) {
            sessionId = request.getParameter(sessionKey);
        }
        return sessionId;
    }

    public static String getSessionId(HttpServletRequest request) {
        return getSessionId(request, Constants.SESSION_KEY);
    }
}
