package com.bilibili.cluster.scheduler.api.service.oa;

import com.bilibili.cluster.scheduler.common.dto.oa.OAForm;
import com.bilibili.cluster.scheduler.common.dto.oa.manager.CodeDiff;
import com.bilibili.cluster.scheduler.common.dto.oa.manager.OaChangeInfo;
import com.bilibili.cluster.scheduler.common.dto.oa.manager.ReplaceRoleModel;

import java.util.List;
import java.util.function.Supplier;

/**
 * @description: oa审批
 * @Date: 2024/3/6 10:56
 * @Author: nizhiqiang
 */
public interface OAService {

    /**
     * 获取token
     *
     * @return
     */
    String getToken();

    /**
     * 查询审批状态
     *
     * @param username
     * @param orderId
     * @return
     */
    OAForm queryForm(String username, String orderId);

    /**
     * 提交审批
     *
     * @param submitUser
     * @param remark
     * @return
     */
    @Deprecated
    OAForm submitFlinkManagerForm(String submitUser, String remark);

    /**
     * 确认是否废弃
     *
     * @param name
     * @param orderId
     * @return
     */
    Boolean checkRefuse(String name, String orderId);

    /**
     * 提交审批
     *
     * @param submitUser   申请人
     * @param remark       文案说明
     * @param approverList 审批人列表
     * @param processName  流程名称
     * @param supplier     formData supplier
     * @return
     */
    @Deprecated
    <T> OAForm submitForm(String submitUser, String remark, List<String> approverList, String processName, Supplier<T> supplier);


    /**
     * 提交审批
     *
     * @param submitUser
     * @param remark
     * @return
     */
    @Deprecated
    OAForm submitSparkManagerForm(String submitUser, String remark);


    /**
     * 统一OA审批入口
     *
     * @param submitUser     申请人
     * @param approverList   审批人
     * @param carbonCopyList 抄送人
     * @param codeDiff       代码对比
     * @param processName    流程名称
     * @param oaChangeInfo   流程填充关键数据
     * @param execTime       预期执行时间，秒
     * @param specificSource 变更实体名称，source_type类型不同，此处填的值也不同
     * @param supplier       组件owner、sre信息
     * @return
     */
    OAForm submitUnifiedForm(String submitUser, List<String> approverList, List<String> carbonCopyList, CodeDiff codeDiff,
                             String processName, OaChangeInfo oaChangeInfo, long execTime, String specificSource,
                             Supplier<ReplaceRoleModel> supplier);


}
