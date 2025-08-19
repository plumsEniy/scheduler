package com.bilibili.cluster.scheduler.common.dto.spark.ess;

import lombok.Data;

import java.util.List;

@Data
public class BlackList {

    private List<String> type;

    private List<String> hosts;

}
