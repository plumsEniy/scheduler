package com.bilibili.cluster.scheduler.common.dto.hdfs.nnproxy.parms;

import com.bilibili.cluster.scheduler.common.enums.flow.FlowUrgencyType;
import com.bilibili.cluster.scheduler.common.enums.flow.SubDeployType;
import lombok.Data;

import java.util.List;

@Data
public class NNProxyDeployFlowExtParams {

    private SubDeployType subDeployType;

    private FlowUrgencyType urgencyType;

    private List<ComponentConfInfo> confInfoList;

    private List<String> approverList;

    private List<String> ccList;
}
