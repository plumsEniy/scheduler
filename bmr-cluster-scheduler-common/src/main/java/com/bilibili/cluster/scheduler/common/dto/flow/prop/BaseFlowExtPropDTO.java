package com.bilibili.cluster.scheduler.common.dto.flow.prop;

import com.bilibili.cluster.scheduler.common.dto.button.DeployStageInfo;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BaseFlowExtPropDTO {

    private List<String> nodeList;

    private String flowExtParams;

    /**
     * 记录阶段发布的统计信息
     */
    private Map<Integer, DeployStageInfo> stageInfos;

    /**
     * 记录阶段可以点击继续的时间
     */
    private String allowedNextProceedTime;
}
