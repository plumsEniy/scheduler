package com.bilibili.cluster.scheduler.api.service.wx;

import cn.hutool.core.net.url.UrlBuilder;
import cn.hutool.http.HttpRequest;
import cn.hutool.json.JSONUtil;
import com.bilibili.cluster.scheduler.common.Constants;
import com.bilibili.cluster.scheduler.common.dto.wx.req.NotifyChangeStartReq;
import com.bilibili.cluster.scheduler.common.dto.wx.req.NotifyChangeStatusReq;
import com.bilibili.cluster.scheduler.common.dto.wx.req.WXPushMsgReq;
import com.bilibili.cluster.scheduler.common.dto.wx.req.WXRobotNotifyReq;
import com.bilibili.cluster.scheduler.common.dto.wx.resp.NotifyChangeStartResp;
import com.bilibili.cluster.scheduler.common.dto.wx.resp.WXRobotNotifyResp;
import com.bilibili.cluster.scheduler.common.dto.wx.resp.WxPushMsgResp;
import com.bilibili.cluster.scheduler.common.enums.wx.ProcessStatusEnum;
import com.bilibili.cluster.scheduler.common.response.BaseResp;
import com.bilibili.cluster.scheduler.common.utils.BaseRespUtil;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @description: 微信变更通知
 * @Date: 2024/3/18 18:12
 * @Author: nizhiqiang
 */

@Slf4j
@Service
public class WxPublisherServiceImpl implements WxPublisherService {

    @Value("${oa-flow.get-user-info-base-url}")
    private String BASE_URL = "https://alter.bilibili.co";


    @Value("#{'${saber.notify.robotsList:}'.split(',')}")
    private List<Integer> saberNotifyRobotsList;

    public static final String QYWX_URI = "https://qyapi.weixin.qq.com";


    @Override
    public Long notifyChangeStart(String changeLog, String username) {
        NotifyChangeStartReq notifyReq = NotifyChangeStartReq.getModel(username, saberNotifyRobotsList);
        notifyReq.setChangelog(changeLog);
        String url = BASE_URL + "/api/alter/publisher/apply";
        String req = JSONUtil.toJsonStr(notifyReq);
        log.info("notify change start req is {}", req);
        String respStr = HttpRequest.post(url)
                .body(req)
                .header(Constants.CONTENT_TYPE, Constants.APPLICATION_JSON)
                .header(Constants.CONTENT_TYPE, Constants.UTF_8)
                .charset(Constants.UTF_8)
                .header(Constants.COOKIE, Constants.SESSION_KEY + "=" + MDC.get(Constants.SESSION_KEY))
                .execute().body();
        NotifyChangeStartResp resp = JSONUtil.toBean(respStr, NotifyChangeStartResp.class);
        log.info("notify change start resp is {}", JSONUtil.toJsonStr(resp));
        BaseRespUtil.checkCommonResp(resp);
        return resp.getData().getId();
    }

    @Override
    public void changeNotifyStatus(Long notifyId, String username, ProcessStatusEnum status) {
        NotifyChangeStatusReq req = new NotifyChangeStatusReq(notifyId, status.getType(), username);
        String url = BASE_URL + "/api/alter/publisher/update_process_status";
        String respStr = HttpRequest.post(url)
                .header(Constants.CONTENT_TYPE, Constants.UTF_8)
                .charset(Constants.UTF_8)
                .body(JSONUtil.toJsonStr(req))
                .execute().body();
        BaseResp resp = JSONUtil.toBean(respStr, BaseResp.class);
        BaseRespUtil.checkCommonResp(resp);
    }

    @Override
    public void wxRobotNotify(String key, String context) {
        String url = UrlBuilder.ofHttp(QYWX_URI)
                .addPath("/cgi-bin/webhook/send")
                .addQuery("key", key)
                .build();
        WXRobotNotifyReq req = new WXRobotNotifyReq(context);
        log.info("wx notify req is {}", JSONUtil.toJsonStr(req));
        String respStr = HttpRequest.post(url)
                .header(Constants.CONTENT_TYPE, Constants.UTF_8)
                .charset(Constants.UTF_8)
                .body(JSONUtil.toJsonStr(req))
                .execute().body();
        WXRobotNotifyResp resp = JSONUtil.toBean(respStr, WXRobotNotifyResp.class);
        log.info("wx notify resp is {}", JSONUtil.toJsonStr(resp));
        BaseRespUtil.checkCommonResp(resp);
    }

    @Override
    public void wxPushMsg(List<String> operatorList, String msgTypeText, String message) {
        String url = UrlBuilder.ofHttp(BASE_URL)
                .addPath("/api/alter/cmdb/open/workwx/message/send")
                .build();
        WXPushMsgReq req = new WXPushMsgReq(operatorList, msgTypeText, message);
        log.info("wx push resp is {}", JSONUtil.toJsonStr(req));
        String respStr = HttpRequest.post(url)
                .header(Constants.CONTENT_TYPE, Constants.UTF_8)
                .header(Constants.TOKEN, Constants.WX_PUSH_TOKEN)
                .charset(Constants.UTF_8)
                .body(JSONUtil.toJsonStr(req))
                .execute().body();
        WxPushMsgResp resp = JSONUtil.toBean(respStr, WxPushMsgResp.class);
        log.info("wx push resp is {}", JSONUtil.toJsonStr(resp));
        BaseRespUtil.checkCommonResp(resp);
    }

}
