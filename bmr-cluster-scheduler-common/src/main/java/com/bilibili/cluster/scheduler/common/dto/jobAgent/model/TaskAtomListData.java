package com.bilibili.cluster.scheduler.common.dto.jobAgent.model;

import com.bilibili.cluster.scheduler.common.dto.jobAgent.TaskAtomDetail;
import lombok.Data;

import java.util.List;

/**
 * @description:
 * @Date: 2024/5/16 10:42
 * @Author: nizhiqiang
 */
@Data
public class TaskAtomListData {
    private List<TaskAtomDetail> records;

}
