package com.bilibili.cluster.scheduler.common.dto.caster;

import com.bilibili.cluster.scheduler.common.enums.resourceV2.TideClusterType;
import lombok.Data;

@Data
public class TaintConf {

    private String key;

    private String value;

    private String effect = "NoSchedule";


    public TaintConf(TideClusterType tideClusterType) {
        this.key = tideClusterType.getTaintKey();
        this.value = tideClusterType.getTaintValue();
    }
}
