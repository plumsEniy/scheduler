package com.bilibili.cluster.scheduler.api.service.tide;

import com.bilibili.cluster.scheduler.api.service.flow.prepare.factory.FlowPrepareGenerateFactory;
import com.bilibili.cluster.scheduler.common.dto.bmr.resourceV2.TideNodeDetail;

import java.util.List;

public interface TideDynamicNodeGenerateService {

    boolean generateTideStage2NodeAndEvents(List<TideNodeDetail> availablePrestoNodeList, long flowId, FlowPrepareGenerateFactory flowPrepareGenerateFactory) throws Exception;

}
