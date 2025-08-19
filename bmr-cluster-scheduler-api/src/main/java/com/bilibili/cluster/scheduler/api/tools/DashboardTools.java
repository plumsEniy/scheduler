package com.bilibili.cluster.scheduler.api.tools;

import com.bilibili.cluster.scheduler.api.model.DashboardLoginUser;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.TreeMap;

import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONUtil;

public class DashboardTools {

    private final static String dashboardCaller = "berserker";

    private final static String dashboardUrl = "http://dashboard-mng.bilibili.co/api/session/verify";

    private final static String dashboardApikey = "c9nsyzdvvmjnc3ihcr6w3esbufuykbxj";

    public static DashboardLoginUser verify(String sessionId) throws UnsupportedEncodingException {
        TreeMap<String, String> params = new TreeMap<>();
        params.put("caller", dashboardCaller);
        params.put("encrypt", "md5");
        params.put("session_id", sessionId);
        params.put("ts", String.valueOf(System.currentTimeMillis()));
        params.put("sign", ApiSignHelper.getSign(params, dashboardApikey));

        HashMap<String, Object> map = new HashMap<>();
        params.forEach((k, v) -> {
            map.put(k, v);
        });
        String response = HttpUtil.get(dashboardUrl, map);

        return JSONUtil.toBean(response, DashboardLoginUser.class);
    }
}
