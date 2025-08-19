package com.bilibili.cluster.scheduler.api.service.scheduler.node;

import com.bilibili.cluster.scheduler.common.dto.spark.params.SparkDeployJobExtParams;
import com.bilibili.cluster.scheduler.common.utils.StageSplitUtil;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ExecutionNodeTestUnit {

    @Test
    public void testNodePageQuery() {
        // List<SparkDeployJobExtParams> jobExtParamsList = Collections.emptyList();
        List<SparkDeployJobExtParams> jobExtParamsList = Arrays.asList(new SparkDeployJobExtParams(), new SparkDeployJobExtParams());
        final Map<Long, SparkDeployJobExtParams> deployJobExtParamsMap = jobExtParamsList.stream().collect(Collectors.toMap(SparkDeployJobExtParams::getNodeId, Function.identity()));
        System.out.println(deployJobExtParamsMap);
    }

    @Test
    public void testGenerateStage() {
        List<String> jobIdList = Arrays.asList("0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11");
        final Map<String, Set<String>> stageMap = StageSplitUtil.buildStageMap(jobIdList, Arrays.asList(1,30,100));

        System.out.println(stageMap);
    }



}
