package com.bilibili.cluster.scheduler.common.dto.yarn;

import lombok.Data;

import java.util.List;

@Data
public class RMInfoObj {

    private int componentId;

    private List<String> rmHostList;

}
