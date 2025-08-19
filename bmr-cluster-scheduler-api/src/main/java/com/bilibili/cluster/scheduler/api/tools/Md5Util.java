package com.bilibili.cluster.scheduler.api.tools;

import cn.hutool.json.JSONUtil;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;

/**
 * @description: md5工具
 * @Date: 2024/1/29 15:23
 * @Author: nizhiqiang
 */
public class Md5Util {

    private Md5Util() {
    }

    public static String getObjectMd5Sign(Object object) {
        String json;
        if (object instanceof String) {
            json = (String) object;
        } else {
            json = JSONUtil.toJsonStr(object);
        }
        return getMD5Hash(json);
    }

    public static String getMD5Hash(String source) {
        return DigestUtils.md5Hex(source);
    }

    public static String encryptPassword(String password) {
        return Base64.encodeBase64String(DigestUtils.md5(password));
    }
}
