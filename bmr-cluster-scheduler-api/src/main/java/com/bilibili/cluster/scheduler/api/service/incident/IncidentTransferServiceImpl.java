package com.bilibili.cluster.scheduler.api.service.incident;

import cn.hutool.core.net.url.UrlBuilder;
import cn.hutool.http.HttpRequest;
import cn.hutool.json.JSONUtil;
import com.bilibili.cluster.scheduler.api.service.bmr.metadata.BmrMetadataService;
import com.bilibili.cluster.scheduler.api.service.flow.ExecutionFlowService;
import com.bilibili.cluster.scheduler.api.service.wx.WxPublisherService;
import com.bilibili.cluster.scheduler.common.Constants;
import com.bilibili.cluster.scheduler.common.dto.incident.req.InitIncidentReq;
import com.bilibili.cluster.scheduler.common.dto.incident.req.UpdateIncidentStatusReq;
import com.bilibili.cluster.scheduler.common.dto.incident.resp.IncidentResp;
import com.bilibili.cluster.scheduler.common.entity.ExecutionFlowEntity;
import com.bilibili.cluster.scheduler.common.enums.flow.FlowDeployType;
import com.bilibili.cluster.scheduler.common.enums.incident.IncidentStatus;
import com.bilibili.cluster.scheduler.common.utils.BaseRespUtil;
import com.bilibili.cluster.scheduler.common.utils.NumberUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.text.SimpleDateFormat;
import java.util.*;


@Slf4j
@Component
public class IncidentTransferServiceImpl implements IncidentTransferService, InitializingBean {

    @Resource
    BmrMetadataService bmrMetadataService;

    private final static SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    @Value("${incident.base-url}")
    private String BASE_URL = "http://incident.bilibili.co";
    @Value("${incident.x-secretid}")
    private String xSecretid = "I6QltRVm5PKHP9g";
    @Value("${incident.x-signature}")
    private String xSignature = "5542b8b0ffbc394e97171d031a9815ee";
    @Value("${incident.env}")
    private int envId = 1;
    @Value("${incident.platform_id}")
    private int platformId = 108;
    @Value("${incident.source_type}")
    private int sourceType = 3;
    @Value("${incident.behavior_id}")
    private String behaviorId = "1h11k3qaakv";

    @Value("${spring.profiles.active}")
    private String env;

    @Resource
    WxPublisherService wechatRobotService;

    @Resource
    ExecutionFlowService executionFlowService;

    Map<String, List<String>> headerMap;

    @Override
    public void startIncident(ExecutionFlowEntity flow) {
        try {
            String url = UrlBuilder.ofHttp(BASE_URL)
                    .addPath("/change-focus/api/v2/control/change/init")
                    .build();

            InitIncidentReq req = getIncidentInitReq(flow);
            log.info("start incident req is {}", JSONUtil.toJsonStr(req));

            String respStr = HttpRequest.post(url)
                    .header(headerMap)
                    .body(JSONUtil.toJsonStr(req))
                    .execute()
                    .body();

            log.info("start incident resp is {}", respStr);
            IncidentResp resp = JSONUtil.toBean(respStr, IncidentResp.class);
            BaseRespUtil.checkCommonResp(resp);

            notifyRobot(flow, IncidentStatus.INIT);

        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    @Override
    public void updateIncidentStatus(ExecutionFlowEntity flow, IncidentStatus status) {
        try {
            UpdateIncidentStatusReq req = new UpdateIncidentStatusReq();
            req.setChangeUuid(flow.getId().toString());
            req.setEnv(env);
            req.setPlatformId(platformId);
            req.setTimestamp(System.currentTimeMillis() / 1000);
            req.setStatus(status.getNum());

            String url = UrlBuilder.ofHttp(BASE_URL)
                    .addPath("/change-focus/api/v2/control/change/finish")
                    .build();

            log.info("finish incident req is {}", JSONUtil.toJsonStr(req));

            String respStr = HttpRequest.post(url)
                    .header(headerMap)
                    .body(JSONUtil.toJsonStr(req))
                    .execute()
                    .body();

            log.info("finish incident resp is {}", respStr);
            IncidentResp resp = JSONUtil.toBean(respStr, IncidentResp.class);
            BaseRespUtil.checkCommonResp(resp);
            notifyRobot(flow, status);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    private void notifyRobot(ExecutionFlowEntity flow, IncidentStatus status) {
        Long componentId = flow.getComponentId();

//        不存在组件id则不发送
        if (!NumberUtils.isPositiveLong(componentId)) {
            return;
        }
        List<String> robotList = bmrMetadataService.queryWechatRobotByComponentId(componentId);

        if (CollectionUtils.isEmpty(robotList)) {
            return;
        }

        for (String key : robotList) {
            wechatRobotService.wxRobotNotify(key, buildIncidentMessage(flow, status));
        }
    }


    private static String getIncidentObject(ExecutionFlowEntity flow) {
        return flow.getRoleName() + "-" + flow.getClusterName() + "-" + flow.getComponentName();
    }

    private static String getIncidentTitle(ExecutionFlowEntity flow) {
        String title = flow.getRoleName() + "-" + flow.getClusterName() + "-" + flow.getComponentName() + "-"
                + flow.getDeployType().getDesc();
        return title;
    }


    private String buildIncidentMessage(ExecutionFlowEntity flow, IncidentStatus status) {
        String url = "https://cloud.bilibili.co/changePilot/search/detail?uuid=";
        if (env.equals(Constants.PROD_ENV)) {
            url += "BMR-prod-" + flow.getId();
        } else if (env.equals(Constants.PRE_ENV)) {
            url += "BMR-pre-" + flow.getId();
        }
        String desc = Constants.EMPTY_STRING;
        switch (status) {

            case INIT:
                desc = String.format("<font color=\"comment\">%s</font>", status.getDesc());
                break;
            case FAIL:
            case GIVE_UP:
            case ROLLBACK:
                desc = String.format("<font color=\"warning\">%s</font>", status.getDesc());
                break;
            case FINISH:
                desc = String.format("<font color=\"info\">%s</font>", status.getDesc());
                break;
            default:
        }
        String date = format.format(new Date());

        String msg = "【变更通告】\n" +
                ">摘要:" + getIncidentTitle(flow) + "\n" +
                ">变更状态:" + desc + "\n" +
                ">变更对象:" + getIncidentObject(flow) + "\n" +
                ">变更平台:" + Constants.INCIDENT_PLATFORM + "\n" +
                ">操作人:" + flow.getOperator() + "\n" +
                ">审批人:" + flow.getApprover() + "\n" +
                ">操作时间:" + date + "\n" +
                ">变更类型:" + flow.getDeployType().getDesc() + "\n" +
                ">详情链接:" + "[查看详情]" + "(" + url + ")" + "\n";
        return msg;
    }

    public InitIncidentReq getIncidentInitReq(ExecutionFlowEntity flow) {
        InitIncidentReq req = new InitIncidentReq();
        String title = getIncidentTitle(flow);

        FlowDeployType deployType = flow.getDeployType();

        String url = executionFlowService.generateFlowUrl(flow);

        String changeTarget = getIncidentObject(flow);
        String description = flow.getFlowRemark() + "\n" +
                "审批人: " + flow.getApprover() + "\n" +
                "变更类型: " + flow.getDeployType().getDesc() + "\n";

        req.setBehaviorId(behaviorId);
        req.setEnv(env);
        req.setTimestamp(System.currentTimeMillis() / 1000);
        req.setRefer(url);
        req.setDesc(description);
        req.setOrderId(flow.getOrderId());
        req.setTitle(title);
        req.setOperator(flow.getOperator());
        req.setChangeUuid(flow.getId().toString());
        req.setPlatformId(platformId);

        req.setChangeTarget(changeTarget);

        return req;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        headerMap = new HashMap<>();
        headerMap.put("x-secretid", Arrays.asList(xSecretid));
        headerMap.put("x-signature", Arrays.asList(xSignature));
    }
}
