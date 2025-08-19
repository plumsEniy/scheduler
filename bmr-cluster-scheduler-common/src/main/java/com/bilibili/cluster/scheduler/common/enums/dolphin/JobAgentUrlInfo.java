package com.bilibili.cluster.scheduler.common.enums.dolphin;

import lombok.Data;

/**
 * @description: 获取jobagent结果
 * @Date: 2024/5/21 10:33
 * @Author: nizhiqiang
 */
@Data
public class JobAgentUrlInfo {
    /**
     * 步骤名
     */
    String stageName;

    /**
     * jobagent链接
     */
    String jobAgentUrl;

    /**
     * dolphin上的状态
     */
    DolphinWorkflowExecutionStatus dolphinStatus;

    public String buildJobAgentUrlStr() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("步骤名\t" + stageName);
        stringBuilder.append("\t");
        stringBuilder.append("状态\t" + dolphinStatus);
        stringBuilder.append("\t");
        stringBuilder.append("脚本执行链接\t" + jobAgentUrl);
        return stringBuilder.toString();
    }


}
