package com.bilibili.cluster.scheduler.common.dto.hdfs.nnproxy.parms;

import lombok.Data;

@Data
public class NNProxyDeployRollbackNodeExtParams extends NNProxyDeployNodeExtParams {

    private long preNodeId;

}
