package com.bilibili.cluster.scheduler.common.dto.hdfs.nnproxy.parms;

import lombok.Data;

@Data
public class ComponentConfInfo {

    private long componentId;

    private String componentName;

    private long configId;

    private String configVersion;

}
