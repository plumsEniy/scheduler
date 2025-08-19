package com.bilibili.cluster.scheduler.common.dto.bmr.config.req;

import com.bilibili.cluster.scheduler.common.dto.bmr.config.model.ConfigItem;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class UpdateSpecialKeyValueFileReq implements Serializable {

    private static final long serialVersionUID = 1L;
    private Long componentId;
    private List<ConfigItem> updateItems;
    private String fileName;
}
