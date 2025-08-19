package com.bilibili.cluster.scheduler.api.service.flow.prepare.factory.spark;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.bilibili.cluster.scheduler.api.event.analyzer.PipelineParameter;
import com.bilibili.cluster.scheduler.api.event.analyzer.ResolvedEvent;
import com.bilibili.cluster.scheduler.api.event.factory.FactoryDiscoveryUtils;
import com.bilibili.cluster.scheduler.api.event.factory.PipelineFactory;
import com.bilibili.cluster.scheduler.api.service.flow.ExecutionFlowPropsService;
import com.bilibili.cluster.scheduler.api.service.flow.ExecutionNodeEventService;
import com.bilibili.cluster.scheduler.api.service.flow.ExecutionNodePropsService;
import com.bilibili.cluster.scheduler.api.service.flow.ExecutionNodeService;
import com.bilibili.cluster.scheduler.api.service.flow.prepare.factory.FlowPrepareGenerateFactory;
import com.bilibili.cluster.scheduler.api.tools.ReadModelUtils;
import com.bilibili.cluster.scheduler.common.Constants;
import com.bilibili.cluster.scheduler.common.dto.bmr.experiment.ExperimentType;
import com.bilibili.cluster.scheduler.common.dto.flow.prop.BaseFlowExtPropDTO;
import com.bilibili.cluster.scheduler.common.dto.bmr.experiment.ExperimentJobProps;
import com.bilibili.cluster.scheduler.common.dto.spark.params.SparkExperimentFlowExtParams;
import com.bilibili.cluster.scheduler.common.entity.ExecutionFlowEntity;
import com.bilibili.cluster.scheduler.common.entity.ExecutionNodeEntity;
import com.bilibili.cluster.scheduler.common.enums.NodeExecuteStatusEnum;
import com.bilibili.cluster.scheduler.common.enums.flow.FlowDeployType;
import com.bilibili.cluster.scheduler.common.enums.node.NodeOperationResult;
import com.google.common.base.Preconditions;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class SparkExperimentFlowPrepareGenerateFactory implements FlowPrepareGenerateFactory, InitializingBean {

    @Resource
    ExecutionNodeService nodeService;

    @Resource
    ExecutionNodeEventService nodeEventService;

    @Resource
    ExecutionNodePropsService nodePropsService;

    @Resource
    ExecutionFlowPropsService flowPropsService;

    // not used
    private JSONObject sparkImageConf;

    @Value("${spark.experiment.use.oneclient:true}")
    boolean useOneclient = true;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void generateNodeAndEvents(ExecutionFlowEntity flowEntity) throws Exception {
        try {
            Long flowId = flowEntity.getId();
            BaseFlowExtPropDTO flowProps = flowPropsService.getFlowPropByFlowId(flowId, BaseFlowExtPropDTO.class);
            String flowExtParams = flowProps.getFlowExtParams();
            SparkExperimentFlowExtParams experimentFlowExtParams = JSONUtil.toBean(flowExtParams, SparkExperimentFlowExtParams.class);

            List<String> jobIdList = flowProps.getNodeList();
            List<ExecutionNodeEntity> executionJobList = new ArrayList<>();
            Map<String, ExperimentJobProps> jobPropsMap = new HashMap<>();

            Integer flowParallelism = flowEntity.getParallelism();
            int batchId = 1;
            int curs = 0;

            for (String jobId : jobIdList) {
                ExecutionNodeEntity jobEntity = new ExecutionNodeEntity();
                jobEntity.setNodeName(jobId);
                jobEntity.setFlowId(flowId);
                jobEntity.setOperator(flowEntity.getOperator());
                jobEntity.setNodeStatus(NodeExecuteStatusEnum.UN_NODE_EXECUTE);
                jobEntity.setBatchId(batchId);
                jobEntity.setRack(Constants.EMPTY_STRING);
                jobEntity.setOperationResult(NodeOperationResult.NORMAL);
                if (++curs >= flowParallelism) {
                    curs = 0;
                    batchId++;
                }
                executionJobList.add(jobEntity);

                ExperimentJobProps jobProps = generateJobProps(jobId, experimentFlowExtParams);
                jobProps.setOpUser(flowEntity.getOperator());
                jobPropsMap.put(jobId, jobProps);
            }

            List<List<ExecutionNodeEntity>> splitList = ListUtil.split(executionJobList, 500);
            for (List<ExecutionNodeEntity> split : splitList) {
                Assert.isTrue(nodeService.batchInsert(split), "批量插入execution node失败");
            }
            log.info("save flow {} execution node list success.", flowId);

            for (ExecutionNodeEntity nodeEntity : executionJobList) {
                final String jobId = nodeEntity.getNodeName();
                ExperimentJobProps jobProps = jobPropsMap.get(jobId);
                Preconditions.checkNotNull(jobProps, "jobProps is null");
                jobProps.setNodeId(nodeEntity.getId());
                nodePropsService.saveNodeProp(nodeEntity.getId(), jobProps);
            }
            log.info("save flow {} execution job props success.", flowId);

            List<ResolvedEvent> resolvedEventList = resolvePipelineEventList(null, flowEntity, null, null);
            log.info("SparkExperimentFlowPrepareGenerateFactory#resolvedEventList is {}", resolvedEventList);

            nodeEventService.batchSaveExecutionNodeEvent(flowId, executionJobList, resolvedEventList);
            log.info("save flow {} execution job event success.", flowId);

        } catch (Exception e) {
            log.error("SparkExperimentFlowPrepareGenerateFactory#generateNodeAndEvents failed: {}", e.getMessage());
            log.error(e.getMessage(), e);
            throw e;
        }
    }

    private ExperimentJobProps generateJobProps(String jobId, SparkExperimentFlowExtParams flowExtParams) {
        final ExperimentJobProps jobProps = new ExperimentJobProps();
        jobProps.setJobId(jobId);
        jobProps.setJobType(flowExtParams.getJobType());
        jobProps.setTestSetVersionId(flowExtParams.getTestSetVersionId());

        ExperimentType experimentType = flowExtParams.getExperimentType();
        jobProps.setExperimentType(flowExtParams.getExperimentType());

        jobProps.setMetrics(flowExtParams.getMetrics());
        jobProps.setCiInstanceId(flowExtParams.getInstanceId());

        String platformA = flowExtParams.getPlatformA();
        jobProps.setPlatformA("spark_3.1");
        Map<String, Object> confAMap = new HashMap<>();
        final String aRunTimeConf = flowExtParams.getARunTimeConf();
        if (!StringUtils.isBlank(aRunTimeConf)) {
            confAMap.putAll(JSONUtil.parseObj(aRunTimeConf));
        }
        if (useOneclient) {
            confAMap.put("spark.oneclient.minorVersion", platformA);
        } else {
            confAMap.put("spark.docker.imageName", flowExtParams.getImageA());
        }
        final String confA = flowExtParams.getConfA();
        if (!StringUtils.isBlank(confA)) {
            confAMap.putAll(JSONUtil.parseObj(confA));
        }
        jobProps.setConfA(JSONUtil.toJsonStr(confAMap));

        switch (experimentType) {
            case PERFORMANCE_TEST:
                jobProps.setPlatformB(Constants.EXPERIMENT_PLATFORM_EMPTY_VALUE);
                break;
            case COMPARATIVE_TASK:
                String platformB = flowExtParams.getPlatformB();
                jobProps.setPlatformB("spark_4.0");
                Map<String, Object> confBMap = new HashMap<>();
                final String bRunTimeConf = flowExtParams.getBRunTimeConf();
                if (!StringUtils.isBlank(bRunTimeConf)) {
                    confBMap.putAll(JSONUtil.parseObj(bRunTimeConf));
                }
                if (useOneclient) {
                    confBMap.put("spark.oneclient.minorVersion", platformB);
                } else {
                    confBMap.put("spark.docker.imageName", flowExtParams.getImageB());
                }
                final String confB = flowExtParams.getConfB();
                if (!StringUtils.isBlank(confB)) {
                    confBMap.putAll(JSONUtil.parseObj(confB));
                }
                jobProps.setConfB(JSONUtil.toJsonStr(confBMap));
                break;
        }
        return jobProps;
    }

    @Override
    public List<FlowDeployType> fitDeployType() {
        return Arrays.asList(FlowDeployType.SPARK_EXPERIMENT);
    }

    @Override
    public List<ResolvedEvent> resolvePipelineEventList(PipelineParameter pipelineParameter) throws Exception {
        PipelineFactory pipelineFactory = FactoryDiscoveryUtils.getFactoryByIdentifier(Constants.SPARK_EXPERIMENT_PIPELINE_FACTORY_IDENTIFY, PipelineFactory.class);
        List<ResolvedEvent> resolvedEventList = pipelineFactory.analyzerAndResolveEvents(pipelineParameter);
        return resolvedEventList;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        ClassPathResource sparkImageClassPath = new ClassPathResource("spark/version.json");
        String json = ReadModelUtils.readModel(sparkImageClassPath);
        log.info("read spark/version.json result is {}", json);
        sparkImageConf = JSONUtil.parseObj(json);
        log.info( "sparkImageConf is {}", sparkImageConf.toString());
        log.info("spark.experiment.use.oneclient conf is {}", useOneclient);
    }
}
