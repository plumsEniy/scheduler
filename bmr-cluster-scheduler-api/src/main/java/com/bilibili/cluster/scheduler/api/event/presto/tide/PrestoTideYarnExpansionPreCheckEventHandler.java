package com.bilibili.cluster.scheduler.api.event.presto.tide;

import cn.hutool.core.collection.CollectionUtil;
import com.bilibili.cluster.scheduler.api.event.tide.AbstractTideYarnExpansionPreCheckEventHandler;
import com.bilibili.cluster.scheduler.api.service.bmr.resourceV2.BmrResourceV2Service;
import com.bilibili.cluster.scheduler.api.service.presto.PrestoService;
import com.bilibili.cluster.scheduler.common.Constants;
import com.bilibili.cluster.scheduler.common.dto.tide.conf.TideConfDTO;
import com.bilibili.cluster.scheduler.common.dto.tide.req.DynamicScalingQueryListPageReq;
import com.bilibili.cluster.scheduler.common.dto.tide.resp.DynamicScalingConfDTO;
import com.bilibili.cluster.scheduler.common.dto.tide.type.DynamicScalingStrategy;
import com.bilibili.cluster.scheduler.common.enums.event.EventTypeEnum;
import com.bilibili.cluster.scheduler.common.enums.resourceV2.TideClusterType;
import com.bilibili.cluster.scheduler.common.utils.LocalDateFormatterUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@Component
public class PrestoTideYarnExpansionPreCheckEventHandler extends AbstractTideYarnExpansionPreCheckEventHandler {

    @Resource
    PrestoService prestoService;

    @Resource
    BmrResourceV2Service resourceV2Service;


    @Override
    public EventTypeEnum getHandleEventType() {
        return EventTypeEnum.PRESTO_YARN_TIDE_EXPANSION_PRE_CHECK;
    }

    @Override
    protected long getTideCasterClusterId() {
        return prestoService.getPrestoCasterClusterId();
    }

    @Override
    protected TideClusterType getTideClusterType() {
        return TideClusterType.PRESTO;
    }

    @Override
    protected String getDeployService() {
        return "Presto";
    }

    @Override
    protected List<TideConfDTO> getRequireCheckTideList() {
        final DynamicScalingQueryListPageReq req = DynamicScalingQueryListPageReq.defaultReq(TideClusterType.PRESTO);
        List<DynamicScalingConfDTO> dynamicScalingConfDTOS = resourceV2Service.queryDynamicScalingConfList(req);

        if (CollectionUtil.isEmpty(dynamicScalingConfDTOS)) {
            return Collections.emptyList();
        }
        boolean isHoliday = resourceV2Service.isHoliday(LocalDateFormatterUtils.formatDate(Constants.FMT_DAY, LocalDate.now()));

        String currentTime = LocalDateFormatterUtils.format(Constants.FMT_MINS, LocalDateTime.now());
        String timeFmt = currentTime.split(" ")[1];
        List<TideConfDTO> expansionList = new ArrayList<>();

        for (DynamicScalingConfDTO dynamicScalingConfDTO : dynamicScalingConfDTOS) {
            final boolean scalingState = dynamicScalingConfDTO.isScalingState();
            if (!scalingState) {
                continue;
            }
            final DynamicScalingStrategy dynamicScalingStrategy = dynamicScalingConfDTO.getDynamicScalingStrategy();

            if (!DynamicScalingStrategy.FIRST_EXPAND_THEN_SHRINK.equals(dynamicScalingStrategy)) {
                continue;
            }
            if (dynamicScalingConfDTO.isSkipHolidayScaling() && isHoliday) {
                continue;
            }

            final String expansionTimeStart = dynamicScalingConfDTO.getExpansionTimeStart();
            if (timeFmt.compareTo(expansionTimeStart) < 0) {
                continue;
            }

            final TideConfDTO tideConfDTO = new TideConfDTO();
            tideConfDTO.setClusterId(dynamicScalingConfDTO.getClusterId());
            tideConfDTO.setComponentId(dynamicScalingConfDTO.getComponentId());
            tideConfDTO.setAppId(dynamicScalingConfDTO.getAppId());
            tideConfDTO.setHighPodNum(dynamicScalingConfDTO.getHighPeakNodeNum());
            tideConfDTO.setLowPodNum(dynamicScalingConfDTO.getLowPeakNodeNum());

            expansionList.add(tideConfDTO);
        }
        return expansionList;
    }
}
