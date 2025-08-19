package com.bilibili.cluster.scheduler.common.dto.bmr.config.resp;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bilibili.cluster.scheduler.common.dto.bmr.config.ComponentConfigVersionEntity;
import com.bilibili.cluster.scheduler.common.response.BaseMsgResp;
import lombok.Data;

@Data
public class QueryPageVersionListResp  extends BaseMsgResp {

    Page<ComponentConfigVersionEntity> obj;
}