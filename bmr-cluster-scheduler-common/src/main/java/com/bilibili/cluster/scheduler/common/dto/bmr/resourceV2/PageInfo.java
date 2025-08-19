package com.bilibili.cluster.scheduler.common.dto.bmr.resourceV2;

import lombok.Data;

import java.util.List;

/**
 * @description:
 * @Date: 2024/9/5 20:58
 * @Author: nizhiqiang
 */

@Data
public class PageInfo<T> {
    private int pageNum;
    private int pageSize;
    private int size;

    private List<T> list;


}
