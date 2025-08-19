package com.bilibili.cluster.scheduler.common.http;

import com.bilibili.cluster.scheduler.common.Constants;
import com.bilibili.cluster.scheduler.common.exception.RequesterException;
import com.google.common.base.Preconditions;
import lombok.extern.slf4j.Slf4j;
import okhttp3.ConnectionPool;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.springframework.util.StringUtils;

import java.util.concurrent.TimeUnit;

@Slf4j
public class OkHttpUtils {

    private static MediaType defaultJsonMediaType = MediaType.parse("application/json; charset=UTF-8");

    private static RequestBody emptyBody = RequestBody.create(defaultJsonMediaType, Constants.EMPTY_STRING);

    private static OkHttpClient okHttpClient = new OkHttpClient.Builder()
            .connectionPool(new ConnectionPool(20, 6, TimeUnit.MINUTES))
            .connectTimeout(30, TimeUnit.SECONDS)
            .callTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build();

    public static String execRequest(Request request, String logMessage) {
        String respJson;
        try (Response response = okHttpClient.newCall(request).execute()) {
            if (response.isSuccessful()) {
                respJson = response.body().string();
            } else {
                String errorMsg = String.format("request error, code is: %s, message is: %s", response.code(), response.message());
                throw new RequesterException(errorMsg);
            }
            log.info("{}, resp is {}", logMessage, respJson);
            Preconditions.checkState(StringUtils.hasText(respJson), "服务端异常，resp为空");
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            if (e instanceof RequesterException) {
                throw (RequesterException) e;
            } else {
                throw new RequesterException(e);
            }
        }
        return respJson;
    }

    public static MediaType getDefaultJsonMediaType() {
        return defaultJsonMediaType;
    }

    public static RequestBody getEmptyBody() {
        return emptyBody;
    }

    public static OkHttpClient getOkHttpClient() {
        return okHttpClient;
    }
}

