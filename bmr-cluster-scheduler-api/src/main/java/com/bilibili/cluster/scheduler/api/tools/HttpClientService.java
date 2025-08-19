package com.bilibili.cluster.scheduler.api.tools;

import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Sets;
import org.apache.commons.collections.CollectionUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHeader;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * HttpClient发送GET、POST请求
 *
 * @Author libin
 * @CreateDate 2018.5.28 16:56
 */
public class HttpClientService {

    private static final Logger LOGGER = LoggerFactory.getLogger(HttpClientService.class);
    /**
     * 返回成功状态码
     */
    private static final Set<Integer> SUCCESS_CODES = Sets.newHashSet(
            HttpStatus.SC_OK,
            HttpStatus.SC_CREATED,
            HttpStatus.SC_ACCEPTED);
    /**
     * 发送GET请求
     *
     * @param url               请求url
     * @param nameValuePairList 请求参数
     * @return JSON或者字符串
     * @throws Exception
     */
    public static <T> T sendGet(String url, List<NameValuePair> nameValuePairList, Map<String, String> headerInfo,
                                boolean returnIsString) throws Exception {
        JSONObject jsonObject = null;
        String result;
        CloseableHttpClient client = null;
        CloseableHttpResponse response = null;
        try {

            client = HttpClients.createDefault();
            URIBuilder uriBuilder = new URIBuilder(url);

            if (CollectionUtils.isNotEmpty(nameValuePairList)) {
                uriBuilder.addParameters(nameValuePairList);
            }
            HttpGet httpGet = new HttpGet(uriBuilder.build());
            httpGet.setHeader(new BasicHeader("Content-Type", "application/x-www-form-urlencoded; charset=utf-8"));
            httpGet.setHeader(new BasicHeader("Accept", "text/plain;charset=utf-8"));
            if (headerInfo != null && !headerInfo.isEmpty()) {
                headerInfo.forEach((k, v) -> httpGet.setHeader(new BasicHeader(k, v)));
            }
            response = client.execute(httpGet);
            int statusCode = response.getStatusLine().getStatusCode();

            if (SUCCESS_CODES.contains(statusCode)) {
                HttpEntity entity = response.getEntity();
                result = EntityUtils.toString(entity, "UTF-8");
                if (returnIsString) {
                    return (T) result;
                }
                try {
                    jsonObject = JSONObject.parseObject(result);
                } catch (Exception e) {
                    e.printStackTrace();
                    throw e;
                }
            } else {
                LOGGER.error("HttpClientService-line: {}, errorMsg: {}", statusCode, "GET请求失败！");
                throw new RuntimeException("Request " + url + " error, error code: " + statusCode);
            }
        } catch (Exception e) {
            LOGGER.error("request exception: ", e);
            throw e;
        } finally {
            if (response != null) {
                response.close();
            }
            if (client != null) {
                client.close();
            }
        }
        return (T) jsonObject;
    }

    public static JSONObject sendGet(String url, List<NameValuePair> nameValuePairList) throws Exception {
        return HttpClientService.sendGet(url, nameValuePairList, null, false);
    }

    /**
     * 发送POST请求
     *
     * @param url
     * @param request
     * @return JSON或者字符串
     * @throws Exception
     */
    public static JSONObject sendPost(String url, Map<String, Object> request) throws Exception {
        JSONObject jsonObject = null;
        CloseableHttpClient client = null;
        CloseableHttpResponse response = null;
        try {
            /**
             *  创建一个httpclient对象
             */
            client = HttpClients.createDefault();
            /**
             * 创建一个post对象
             */
            HttpPost post = new HttpPost(url);
            /**
             * 包装成一个Entity对象
             */
            StringEntity entity = new StringEntity(JSONObject.toJSONString(request), "UTF-8");
            /**
             * 设置请求的内容
             */
            post.setEntity(entity);
            /**
             * 设置请求的报文头部的编码
             */
            post.setHeader(new BasicHeader("Content-Type", "application/json;"));
            /**
             * 设置请求的报文头部的编码
             */
//            post.setHeader(new BasicHeader("Accept", "text/plain;charset=utf-8"));
            /**
             * 执行post请求
             */
            response = client.execute(post);
            /**
             * 获取响应码
             */
            int statusCode = response.getStatusLine().getStatusCode();
            if (SUCCESS_CODES.contains(statusCode)) {
                /**
                 * 通过EntityUitls获取返回内容
                 */
                HttpEntity httpEntity = response.getEntity();
                String result = EntityUtils.toString(httpEntity, "UTF-8");
                /**
                 * 转换成json,根据合法性返回json或者字符串
                 */
                try {
                    jsonObject = JSONObject.parseObject(result);
                    return jsonObject;
                } catch (Exception e) {
                    e.printStackTrace();
                    throw e;
                }
            } else {
                LOGGER.error("HttpClientService-line: {}, errorMsg：{}", statusCode, "POST请求失败！");
                throw new RuntimeException("Request " + url + " error, error code: " + statusCode);
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        } finally {
            if (response != null) {
                response.close();
            }
            if (client != null) {
                client.close();
            }
        }
    }
}