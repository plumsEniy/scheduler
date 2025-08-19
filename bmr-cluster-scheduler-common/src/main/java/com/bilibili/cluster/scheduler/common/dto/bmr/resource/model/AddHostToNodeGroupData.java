package com.bilibili.cluster.scheduler.common.dto.bmr.resource.model;

import lombok.Data;

import java.util.List;

@Data
public class AddHostToNodeGroupData {

    private List<String> notExistHostList;

}
