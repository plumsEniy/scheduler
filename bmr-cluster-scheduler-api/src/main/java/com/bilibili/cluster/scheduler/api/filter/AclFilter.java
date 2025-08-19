package com.bilibili.cluster.scheduler.api.filter;

import cn.hutool.json.JSONUtil;
import com.bilibili.cluster.scheduler.api.tools.DashboardTools;
import com.bilibili.cluster.scheduler.api.model.DashboardLoginUser;
import com.bilibili.cluster.scheduler.api.tools.RequestTools;
import com.bilibili.cluster.scheduler.api.tools.WebTools;
import com.bilibili.cluster.scheduler.common.Constants;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.bilibili.cluster.scheduler.common.response.ResponseResult;
import com.bilibili.cluster.scheduler.common.utils.RequestUtils;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import cn.hutool.core.util.IdUtil;

@Slf4j
@Component
public class AclFilter extends OncePerRequestFilter {

    private static String URI_SUB_START = "/";

    private static String BMR_SYSTEM_INVOKER_KEY = "BMR-xxxxxxx-TOKEN";
    private static String BMR_SYSTEM_TOKEN = "xxxxxxxxxxxxxxxx";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            request.setCharacterEncoding("utf-8");
            response.setHeader("Access-Control-Allow-Origin", "*");
            response.setHeader("Access-Control-Allow-Methods", "POST, GET, OPTIONS, DELETE");
            response.setHeader("Access-Control-Max-Age", "3600");
            response.setHeader("Access-Control-Allow-Headers",
                    "x-requested-with,content-type,Authorization,Cache-Control");
            response.setHeader("Content-Type", "application/json");


            MDC.put(Constants.LOG_TRACE_ID, IdUtil.simpleUUID());
            String uri = WebTools.getUri(request);
            if (uri.startsWith(URI_SUB_START)) {
                uri = uri.substring(1);
            }

            // 忽略健康检查的请求
            if (uri.contains("monitor/ping") || uri.contains("health")) {
                filterChain.doFilter(request, response);
                return;
            }

            // 忽略swagger的请求
            if (uri.contains("swagger") || uri.contains("api-docs") || uri.contains("refresh")) {
                filterChain.doFilter(request, response);
                return;
            }

            String sessionId = RequestTools.getSessionId(request);
            log.debug("request url:{}", uri);
            DashboardLoginUser dashboardLoginUser = DashboardTools.verify(sessionId);
            if (dashboardLoginUser.getCode() == 0) {
                MDC.put(Constants.SESSION_KEY, sessionId);
                MDC.put(Constants.REQUEST_USER, dashboardLoginUser.getUsername());
            } else {
                // TODO 这里可以进行token合法性验证
                String curInvokerToken = RequestUtils.getSessionId(request, BMR_SYSTEM_INVOKER_KEY);
                if (!BMR_SYSTEM_TOKEN.equals(curInvokerToken)) {
                    response.getWriter().write(JSONUtil.toJsonStr(ResponseResult.unLogin()));
                    return;
                }
                MDC.put(Constants.REQUEST_USER, BMR_SYSTEM_INVOKER_KEY);
                MDC.put(Constants.SESSION_KEY, BMR_SYSTEM_TOKEN);
            }
            filterChain.doFilter(request, response);
        } catch (Exception e) {
            log.error("acl filter occur error", e);
            throw e;
        } finally {
            MDC.remove(Constants.SESSION_KEY);
            MDC.remove(Constants.REQUEST_USER);
            MDC.remove(Constants.LOG_TRACE_ID);
        }
    }

}
