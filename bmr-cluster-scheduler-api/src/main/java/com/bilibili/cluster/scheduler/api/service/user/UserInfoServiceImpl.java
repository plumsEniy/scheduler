package com.bilibili.cluster.scheduler.api.service.user;

import cn.hutool.core.net.url.UrlBuilder;
import cn.hutool.http.HttpRequest;
import cn.hutool.json.JSONUtil;
import com.bilibili.cluster.scheduler.common.dto.user.DutyDetail;
import com.bilibili.cluster.scheduler.common.dto.user.resp.QueryDutyDetailResp;
import com.bilibili.cluster.scheduler.common.utils.ObjectMapperUtil;
import com.bilibili.cluster.scheduler.common.Constants;
import com.bilibili.cluster.scheduler.common.dto.user.UserInfo;
import com.bilibili.cluster.scheduler.common.dto.user.resp.QueryUserInfoResp;
import com.bilibili.cluster.scheduler.common.utils.BaseRespUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.Header;
import org.apache.http.message.BasicHeader;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * @description: 根据英语名查询用户信息
 * @Date: 2024/3/6 14:54
 * @Author: nizhiqiang
 */

@Service
@Slf4j
public class UserInfoServiceImpl implements UserInfoService {

    @Value("${oa-flow.get-user-info-base-url}")
    private String GET_USER_INFO_BASE_URL = "https://alter.bilibili.co";

    private static String token = "jIjnIXdGleP2jH2fictPDLvATtACBN5U";

    private Header[] authHeaders = new Header[]{
            new BasicHeader("token", "jIjnIXdGleP2jH2fictPDLvATtACBN5U"),
            new BasicHeader(Constants.COOKIE, Constants.SESSION_KEY + "=" + MDC.get(Constants.SESSION_KEY)),
    };

    ObjectMapper mapper = ObjectMapperUtil.getCommonObjectMapper();

    @Override
    public UserInfo getUserInfoByEnglishName(String englishName) {
        UserInfo user = queryUserInfoByEnglishName(englishName);
        return user;
    }

    public UserInfo queryUserInfoByEnglishName(String username) {

        String url = GET_USER_INFO_BASE_URL + "/api/alter/cmdb/open/resource/user/describe";
        url += Constants.QUESTION_MARK;
        url = url + Constants.AND + "english_name" + Constants.EQUAL + username;
        String respStr = HttpRequest.get(url)
                .header(Constants.TOKEN, token)
                .header(Constants.COOKIE, Constants.SESSION_KEY + "=" + MDC.get(Constants.SESSION_KEY))
                .execute().body();

        QueryUserInfoResp resp = JSONUtil.toBean(respStr, QueryUserInfoResp.class);
        try {
            BaseRespUtil.checkCommonResp(resp);
        } catch (Exception e) {
            if (-404 == resp.getCode() || resp == null) {
                log.error("can not find user info, user is " + username);
                return null;
            } else {
                throw e;
            }
        }
        return resp.getData().getUser();
    }


    @Override
    public List<UserInfo> getUserInfoByEnglishNameList(String... englishNameList) {
        List<UserInfo> userInfoList = new ArrayList<>();
        for (String englishName : englishNameList) {
            UserInfo userInfo = getUserInfoByEnglishName(englishName);
            if (userInfo != null) {
                userInfoList.add(userInfo);
            }
        }
        return userInfoList;
    }

    @Override
    public List<UserInfo> getUserInfoByEnglishNameList(List<String> englishNameList) {
        return getUserInfoByEnglishNameList(englishNameList.toArray(new String[englishNameList.size()]));
    }

    @Override
    public DutyDetail queryDutyDetail(String team, Long teamId) {
        String url = UrlBuilder.ofHttp(GET_USER_INFO_BASE_URL)
                .setHost("alter.bilibili.co")
                .addPath("/api/alter/rule/open/duty/component/detail/v2")
                .addQuery("team", team)
                .addQuery("id", teamId.toString()).build();
        String respStr = HttpRequest.get(url)
                .header(Constants.TENANT_TOKEN_KEY, Constants.QUERY_DUTY_OPERATOR_TOKEN)
                .execute()
                .body();

        QueryDutyDetailResp resp = JSONUtil.toBean(respStr, QueryDutyDetailResp.class);
        BaseRespUtil.checkCommonResp(resp);
        return resp.getData();
    }
}
