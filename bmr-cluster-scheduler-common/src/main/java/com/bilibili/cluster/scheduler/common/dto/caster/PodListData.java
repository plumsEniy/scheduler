package com.bilibili.cluster.scheduler.common.dto.caster;

import cn.hutool.core.annotation.Alias;
import lombok.Data;

import java.util.List;

@Data
public class PodListData {

    private int running;

    private int total;

    @Alias("pods_resource")
    private List<PodInfo> podsResource;

}
