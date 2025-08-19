package com.bilibili.cluster.scheduler.common.dto.bmr.resource;

import lombok.Data;

import java.util.List;

/**
 * @description: resource的分页
 * @Date: 2024/5/23 09:03
 * @Author: nizhiqiang
 */
@Data
public class BaseResourcePage<T> {
    /**
     * 当前页面
     */
    private int pageNum;

    /**
     * 页面大小
     */
    private int pageSize;

    /**
     * 查询结果个数
     */
    private int size;

    /**
     * 总记录数
     */
    private int total;

    /**
     * 总共有多少页 total / pageSize 向上取整
     */
    private int pages;

    /**
     * 记录list
     */
    private List<T> list;
}