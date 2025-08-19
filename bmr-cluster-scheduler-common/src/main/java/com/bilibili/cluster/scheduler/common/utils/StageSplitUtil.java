package com.bilibili.cluster.scheduler.common.utils;

import com.bilibili.cluster.scheduler.common.Constants;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class StageSplitUtil {

    public static Map<String, Set<String>> buildStageMap(List<String> jobIdList, List<Integer> percentStageList) {
        Map<String, Set<String>> stageWithJobs = new LinkedHashMap<>();
        // 根据stage划分，每个stage至少一个作业
        int execStage = 1;
        int preIndex = 0;
        int jobSize = jobIdList.size();
        int maxIndex = jobSize - 1;

        for (Integer stagePercent : percentStageList) {
            int stepLength;
            if (stagePercent >= 100) {
                stepLength = jobSize - preIndex;
            } else {
                Double floor = Math.floor(stagePercent.intValue() * jobSize * Constants.PERCENT_FACTOR);
                stepLength = floor.intValue() - preIndex;
            }
            if (stepLength <= 0) {
                stepLength = 1;
            }

            Set<String> stageJobs = new LinkedHashSet<>();
            int i = preIndex;
            preIndex += stepLength;
            if (preIndex > maxIndex) {
                preIndex = maxIndex + 1;
            }
            for (; i < preIndex; i++) {
                stageJobs.add(jobIdList.get(i));
            }
            stageWithJobs.put(String.valueOf(execStage), stageJobs);
            if (preIndex > maxIndex) {
                break;
            }
            execStage++;
        }
        return stageWithJobs;
    }


}
