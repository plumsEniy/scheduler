package com.bilibili.cluster.scheduler.common.dto.oa.resp;

import com.bilibili.cluster.scheduler.common.dto.oa.OAHistory;
import lombok.Data;

import java.util.List;

/**
 * @description: 查询oa历史
 * @Date: 2024/3/6 14:26
 * @Author: nizhiqiang
 */
@Data
public class QueryOAFormHistoryResp extends BaseOAResp {

    private OAHistoryData data;

    @Data
    public static class OAHistoryData {

        private List<OAHistory> list;

    }
}
