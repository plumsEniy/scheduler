package com.bilibili.cluster.scheduler.api.service.oa;

import cn.hutool.core.lang.UUID;
import cn.hutool.http.HttpRequest;
import cn.hutool.json.JSONUtil;
import com.bilibili.cluster.scheduler.api.service.user.UserInfoService;
import com.bilibili.cluster.scheduler.common.Constants;
import com.bilibili.cluster.scheduler.common.dto.oa.OAForm;
import com.bilibili.cluster.scheduler.common.dto.oa.OAHistory;
import com.bilibili.cluster.scheduler.common.dto.oa.OAUserContent;
import com.bilibili.cluster.scheduler.common.dto.oa.manager.*;
import com.bilibili.cluster.scheduler.common.dto.oa.manager.flink.FlinkManagerReqForm;
import com.bilibili.cluster.scheduler.common.dto.oa.manager.spark.SparkManagerReqForm;
import com.bilibili.cluster.scheduler.common.dto.oa.req.GetOATokenReq;
import com.bilibili.cluster.scheduler.common.dto.oa.req.SubmitOAFormReq;
import com.bilibili.cluster.scheduler.common.dto.oa.resp.BaseOAResp;
import com.bilibili.cluster.scheduler.common.dto.oa.resp.GetOATokenResp;
import com.bilibili.cluster.scheduler.common.dto.oa.resp.QueryOAFormHistoryResp;
import com.bilibili.cluster.scheduler.common.dto.oa.resp.QueryOAFormResp;
import com.bilibili.cluster.scheduler.common.dto.oa.resp.SubmitOAFormResp;
import com.bilibili.cluster.scheduler.common.dto.user.UserInfo;
import com.bilibili.cluster.scheduler.common.enums.oa.OAFormStatus;
import com.bilibili.cluster.scheduler.common.exception.OAInvokerException;
import com.bilibili.cluster.scheduler.common.exception.RequesterException;
import com.google.common.base.Preconditions;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * @description: oa审批服务
 * @Date: 2024/3/6 11:27
 * @Author: nizhiqiang
 */
@Service
@Slf4j
public class OAServiceImpl implements OAService {

    @Value("${oa-flow.base-url}")
    private String BASE_URL = "https://uat-eeapi.bilibili.co";
    @Value("${oa-flow.token-app-id}")
    private String appId = "e2a7d180635f468fb8eebb4d6f79d235";
    @Value("${oa-flow.token-app-secret}")
    private String appSecret = "6rubrzJcMNgepCRD3146C_MK8q7fWvkGtq_omNd9QRg=";
    private String token;
    private static final String CONTENT_TYPE = "Content-Type";
    @Value("${oa-flow.approver-list}")
    private String[] approverList;

    @Value("${oa-flow.block.x-secretid}")
    private String blockSecretId;
    @Value("${oa-flow.block.x-signature}")
    private String blockSignature;
    @Value("${oa-flow.block.env}")
    private int blockEnv;
    @Value("${oa-flow.block.platform_id}")
    private int platformId;

    @Value("${oa-flow.block.platform_behavior_id}")
    private String platformBehaviorId;

    @Value("${oa-flow.block.app_id}")
    private String blockAppId;
    @Value("${oa-flow.block.source_type}")
    private int blockSourceType;


    @Resource
    private UserInfoService userInfoService;

    @Override
    public String getToken() {
        String url = BASE_URL + "/open-api/auth/open/appAccessToken";
        GetOATokenReq req = new GetOATokenReq(appId, appSecret);
        String respStr = HttpRequest.post(url)
                .header(Constants.CONTENT_TYPE, Constants.APPLICATION_JSON)
                .body(JSONUtil.toJsonStr(req))
                .execute().body();
        // log.info("oa token resp is {}", respStr);
        GetOATokenResp resp = JSONUtil.toBean(respStr, GetOATokenResp.class);
        checkOaResp(resp);
        return resp.getData().get("token");
    }

    @Override
    public OAForm queryForm(String username, String orderId) {
        String url = BASE_URL + "/open-api/flow-open/v1/order/detail";
        url += Constants.QUESTION_MARK;
        url += "orderId" + Constants.EQUAL + orderId;
        HttpRequest httpRequest = HttpRequest.get(url);
        initAuthHttpHeader(httpRequest, username);
        String respStr = httpRequest.timeout(20_000).execute().body();
        QueryOAFormResp resp;
        try {
            resp = JSONUtil.toBean(respStr, QueryOAFormResp.class);
        } catch (Exception e) {
            log.error("query oa result error, url is:\n {}, resp is:\n {}", respStr, url);
            if (respStr.contains("so you are seeing this as a fallback")) {
                // skip this error
                log.error("ignore this query oa error...");
                OAForm oaForm = new OAForm();
                oaForm.setStatus(OAFormStatus.UNDER_APPROVAL);
                return oaForm;
            }
            throw e;
        }
        if (Objects.isNull(resp) || resp.getCode() != 0) {
            log.error("request error : {}, order id is {}", resp, orderId);
            return null;
        }
        Boolean refuse = checkRefuse(username, orderId);
        return new OAForm(resp, refuse);
    }

    @Override
    public OAForm submitFlinkManagerForm(String submitUser, String remark) {
        return submitForm(submitUser, remark, Arrays.asList(approverList), Constants.FLINK_MANAGER_PROCESS_NAME, FlinkManagerReqForm::new);
    }

    @Override
    public Boolean checkRefuse(String username, String orderId) {
        //只有单据的发起人、待办人、已办人和被抄送人可以有权限查看单据，原则上只传发起人名字
        //如果单据存在驳回操作，则返回true

        String url = BASE_URL + "/open-api/flow-open/v1/order/histList";
        url += Constants.QUESTION_MARK;
        url += "orderId" + Constants.EQUAL + orderId;
        HttpRequest httpRequest = HttpRequest.get(url);
        initAuthHttpHeader(httpRequest, username);
        String respStr = httpRequest.timeout(20_000)
                .execute().body();
        QueryOAFormHistoryResp resp;
        try {
            resp = JSONUtil.toBean(respStr, QueryOAFormHistoryResp.class);
        } catch (Exception e) {
            log.error("query oa order history error, url is {}, resp is {}", url, respStr);
            if (respStr.contains("so you are seeing this as a fallback")) {
                return false;
            }
            throw e;
        }

        checkOaResp(resp);
        for (OAHistory list : resp.getData().getList()) {
            if (list.getTaskAction().equals("驳回")) {
                return true;
            }
        }
        return false;
    }

    @Deprecated
    @Override
    public <T> OAForm submitForm(String submitUser, String remark, List<String> approverList, String processName, Supplier<T> supplier) {
        UserInfo submiterInfo = userInfoService.getUserInfoByEnglishName(submitUser);
        List<UserInfo> approverInfoList = userInfoService.getUserInfoByEnglishNameList(approverList);
        Preconditions.checkState(!CollectionUtils.isEmpty(approverInfoList),
                String.format("approvers can not be null, approvers are %s, remark is %s", approverList, remark));

        try {
            SubmitOAFormReq req = new SubmitOAFormReq(submiterInfo, approverInfoList, remark, supplier);
            req.setProcessName(processName);
            String body = JSONUtil.toJsonStr(req);
            log.info("req data is {}", body);

            String url = BASE_URL + "/open-api/flow-open/v1/order/start";
            HttpRequest httpRequest = HttpRequest.post(url);
            initAuthHttpHeader(httpRequest, submitUser);
            String respStr = httpRequest.body(body).execute().body();

            SubmitOAFormResp resp = JSONUtil.toBean(respStr, SubmitOAFormResp.class);
            log.info("resp is {}", JSONUtil.toJsonStr(resp));
            checkOaResp(resp);
            OAForm oaForm = new OAForm(resp);
            oaForm.setProcessName(processName);
            return oaForm;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new OAInvokerException(e);
        }
    }

    @Deprecated
    @Override
    public OAForm submitSparkManagerForm(String submitUser, String remark) {
        return submitForm(submitUser, remark, Arrays.asList(approverList), Constants.SPARK_MANAGER_PROCESS_NAME, SparkManagerReqForm::new);

    }

    @Override
    public OAForm submitUnifiedForm(String submitUser, List<String> approverList, List<String> carbonCopyList, CodeDiff codeDiff,
                                    String processName, OaChangeInfo oaChangeInfo, long execTime, String specificSource,
                                    Supplier<ReplaceRoleModel> supplier) {
        try {
            OAUserContent submiterInfo = wrapperUserInfo(submitUser);
            List<OAUserContent> approverInfoList = wrapperUserInfo(approverList);
            List<OAUserContent> carbonCopyInfoList = wrapperUserInfo(carbonCopyList);

            final UnifiedReqForm unifiedReqForm = new UnifiedReqForm();
            BeanUtils.copyProperties(oaChangeInfo, unifiedReqForm);
            unifiedReqForm.setSubmitUserContents(Arrays.asList(submiterInfo));
            unifiedReqForm.setApproverContents(approverInfoList);
            unifiedReqForm.setCarbonCopyContents(carbonCopyInfoList);
            if (!Objects.isNull(codeDiff)) {
                unifiedReqForm.setCodeDiff(codeDiff);
                unifiedReqForm.setUseCodeDiff(Constants.TRUE);
            }

            // 填充封网审批信息
            BlockRequireData blockData = new BlockRequireData();
            blockData.setPlatformId(platformId);
            blockData.setCuuid(UUID.fastUUID().toString().replace(Constants.BAR, Constants.EMPTY_STRING));
            blockData.setExecTimeUnix(execTime);
            blockData.setEnv(blockEnv);
            blockData.setOperator(submitUser);
            blockData.setSourceType(blockSourceType);
            blockData.setSpecificSource(specificSource);
            blockData.setBehaviorId(platformBehaviorId);
            blockData.setReplaceRole(supplier.get());

            unifiedReqForm.setBlock(JSONUtil.toJsonStr(blockData));

            SubmitOAFormReq req = new SubmitOAFormReq(unifiedReqForm);
            req.setProcessName(processName);
            String body = JSONUtil.toJsonStr(req);
            log.info("submitUnifiedForm req data is {}", body);

            String url = BASE_URL + "/open-api/flow-open/v1/order/start";
            HttpRequest httpRequest = HttpRequest.post(url);
            initAuthHttpHeader(httpRequest, submitUser);
            String respStr = httpRequest.body(body).execute().body();

            SubmitOAFormResp resp = JSONUtil.toBean(respStr, SubmitOAFormResp.class);
            log.info("submitUnifiedForm resp is {}", JSONUtil.toJsonStr(resp));
            checkOaResp(resp);
            OAForm oaForm = new OAForm(resp);
            oaForm.setProcessName(processName);
            return oaForm;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new OAInvokerException(e);
        }
    }

    private List<OAUserContent> wrapperUserInfo(List<String> userList) {
        if (CollectionUtils.isEmpty(userList)) {
            return Collections.emptyList();
        }
        List<OAUserContent> userContents = new LinkedList<>();
        for (String user : userList) {
            UserInfo userInfo = userInfoService.getUserInfoByEnglishName(user);
            if (!Objects.isNull(userInfo) && StringUtils.isNotBlank(userInfo.getEnglishName())) {
                userContents.add(new OAUserContent(userInfo));
            }
        }
        return userContents;
    }

    private OAUserContent wrapperUserInfo(String user) {
        UserInfo userInfo = userInfoService.getUserInfoByEnglishName(user);
        return new OAUserContent(userInfo);
    }

    private void checkOaResp(BaseOAResp baseResp) {
        if (Objects.isNull(baseResp) || baseResp.getCode() != 0) {
            log.error("request error : {}", baseResp);
            throw new RequesterException(baseResp == null ? "response is null!" : baseResp.getMessage());
        }
        baseResp.setSuccess(true);
    }

    private HttpRequest initAuthHttpHeader(HttpRequest httpRequest, String username) {
        String token = getToken();
        return httpRequest.header("BEE-AppAccessToken", "Bearer " + token)
                .header("Content-Type", "application/json; charset=utf-8")
                .header("X-Account", username);
    }

}
