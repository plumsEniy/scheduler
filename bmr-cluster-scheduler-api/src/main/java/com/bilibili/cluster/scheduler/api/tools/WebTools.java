package com.bilibili.cluster.scheduler.api.tools;

import com.bilibili.cluster.scheduler.common.Constants;

import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.http.HttpServletRequest;

import com.bilibili.cluster.scheduler.common.utils.ObjectTools;
import org.springframework.web.util.WebUtils;

/**
 * @author: liuguohui
 * @date: 2018/10/25 0025
 * description:
 */
public class WebTools extends WebUtils {

    public static final String WEB_APP_ROOT_PATH_PARAM = "webAppRootPath";
    private static final String ROOT_CONTEXT = "/";
    private static String contextPath;

    /**
     * 取得不包含context的uri,ex:/ad/index.do(/ad为context)则返回/index.do
     *
     * @param request
     * @return
     */
    public static String getUri(HttpServletRequest request) {
        return getUri(request, false);
    }

    public static String getUri(HttpServletRequest request, boolean withParams) {
        if (contextPath == null) {
            contextPath = "" + request.getContextPath();
        }
        String uri = request.getRequestURI();
        if (!(contextPath.equals(ROOT_CONTEXT))) {
            uri = uri.replaceFirst(contextPath, "");
        }
        uri = uri.replaceAll(Constants.MULTI_SLASH, Constants.SLASH);
        if (!withParams) {
            return uri;
        }
        StringBuilder sb = new StringBuilder(uri).append("?");
        Map<String, String[]> params = request.getParameterMap();
        for (Entry<String, String[]> en : params.entrySet()) {
            String[] values = en.getValue();
            if (ObjectTools.isEmpty(values)) {
                continue;
            }
            for (String value : values) {
                sb.append(en.getKey()).append("=").append(value).append("&");
            }
        }
        return sb.toString();
    }

}
