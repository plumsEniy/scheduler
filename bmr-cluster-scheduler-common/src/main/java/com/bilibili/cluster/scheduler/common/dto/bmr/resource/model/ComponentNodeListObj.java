package com.bilibili.cluster.scheduler.common.dto.bmr.resource.model;

import com.bilibili.cluster.scheduler.common.dto.bmr.resource.ComponentNodeDetail;
import lombok.Data;

import java.util.List;

@Data
public class ComponentNodeListObj {

    private int pageNum;
    private int pageSize;
    private int size;
    private int total;
    private int pages;
    private List<ComponentNodeDetail> list;

}
