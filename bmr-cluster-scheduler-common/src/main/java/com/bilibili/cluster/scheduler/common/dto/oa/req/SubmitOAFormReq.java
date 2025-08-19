package com.bilibili.cluster.scheduler.common.dto.oa.req;

import cn.hutool.json.JSONUtil;
import com.bilibili.cluster.scheduler.common.dto.oa.OAUserContent;
import com.bilibili.cluster.scheduler.common.dto.oa.manager.CodeDiff;
import com.bilibili.cluster.scheduler.common.dto.oa.manager.UnifiedReqForm;
import com.bilibili.cluster.scheduler.common.dto.user.UserInfo;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;

import java.util.LinkedList;
import java.util.List;
import java.util.function.Supplier;

/**
 * @description: 提交oa审批
 * @Date: 2024/3/6 14:38
 * @Author: nizhiqiang
 */

@Data
@Slf4j
public class SubmitOAFormReq<T> {

    private String processName;
    private Args<T> args;

    public SubmitOAFormReq(UserInfo submitUser, List<UserInfo> approverList, String remark, Supplier<T> formSupplier) throws Exception {

        //构建审批人信息
        LinkedList<OAUserContent> approverContents = new LinkedList<>();
        for (UserInfo approver : approverList) {
            approverContents.add(new OAUserContent(approver));
        }

        //构建发布人信息
        LinkedList<OAUserContent> submitUserContents = new LinkedList<>();
        submitUserContents.add(new OAUserContent(submitUser));

        //构建form
        Form form = new Form();
        form.setRemark(remark);
        form.setSubmitUserContents(submitUserContents);
        form.setApproverContents(approverContents);
        log.info("forms:" + JSONUtil.toJsonStr(form));
        final T formInstance = formSupplier.get();
        BeanUtils.copyProperties(form, formInstance);

        //构建args
        this.args = new Args(formInstance);
    }


    public SubmitOAFormReq(UnifiedReqForm unifiedReqForm) throws Exception {
        log.info("forms:" + JSONUtil.toJsonStr(unifiedReqForm));
        //构建args
        this.args = new Args(unifiedReqForm);
    }

    @Data
    @AllArgsConstructor
    public static class Args<T> {
        private T form;
    }

    @Data
    public static class Form {

        private String remark;

        private LinkedList<OAUserContent> submitUserContents;

        private LinkedList<OAUserContent> approverContents;

        private CodeDiff codeDiff;

        private String useCodeDiff;

    }

}
