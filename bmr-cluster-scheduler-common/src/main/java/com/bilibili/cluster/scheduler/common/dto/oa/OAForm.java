package com.bilibili.cluster.scheduler.common.dto.oa;

import com.bilibili.cluster.scheduler.common.dto.oa.resp.QueryOAFormResp;
import com.bilibili.cluster.scheduler.common.dto.oa.resp.SubmitOAFormResp;
import com.bilibili.cluster.scheduler.common.enums.oa.OAFormStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @description: 工作流审批表单
 * @Date: 2024/3/6 10:58
 * @Author: nizhiqiang
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class OAForm {

    /**
     * 流程名
     */
    private String processName;

    /**
     * 提交者
     */
    private String submitUser;

    /**
     * 审批者
     */
    private String approver;

    /**
     * 变更申请单流水号
     */
    private String orderNo;

    /**
     * 变更申请单id
     */
    private String orderId;

    /**
     * 发布变更说明
     */
    private String remark;

    /**
     * 审批状态
     */
    private OAFormStatus status;

    public OAForm(String submitUser, String approver, String remark) {
        this.submitUser = submitUser;
        this.approver = approver;
        this.remark = remark;
    }

    public OAForm(SubmitOAFormResp submitFlowResp) {
        SubmitArgs args = submitFlowResp.getData().getArgs();
//        this.submitUser = args.getForm().getSubmitInfo().get(0).getKey();
//        this.approver = args.getForm().getApproverInfoList().get(0).getKey();
//        this.remark = args.getForm().getTextarea();
        this.orderId = args.getBasicInfo().getOrderId();
        this.orderNo = args.getBasicInfo().getOrderNo();
        this.status = OAFormStatus.UNDER_APPROVAL;
    }

    public OAForm(QueryOAFormResp queryOAFormResp, Boolean refuse) {
        QueryOAFormData data = queryOAFormResp.getData();
        this.orderId = data.getBasicInfo().getOrderId();
        this.orderNo = data.getBasicInfo().getOrderNo();
        String state = data.getBasicInfo().getOrderType();
        if (state.equals("待我处理") || state.equals("审批中")) {
            this.status = OAFormStatus.UNDER_APPROVAL;
        } else if (state.equals("已完结")) {
            this.status = OAFormStatus.APPROVED;
        } else {
            this.status = OAFormStatus.DISCARDED;
        }
        if (refuse) {
            this.status = OAFormStatus.DISCARDED;
        }
    }
}
