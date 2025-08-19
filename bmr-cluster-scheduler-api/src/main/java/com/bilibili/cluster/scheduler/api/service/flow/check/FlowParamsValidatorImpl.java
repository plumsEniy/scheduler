package com.bilibili.cluster.scheduler.api.service.flow.check;

import cn.hutool.json.JSONUtil;
import com.bilibili.cluster.scheduler.api.service.bmr.config.BmrConfigService;
import com.bilibili.cluster.scheduler.api.service.bmr.metadata.BmrMetadataService;
import com.bilibili.cluster.scheduler.api.service.bmr.resource.BmrResourceService;
import com.bilibili.cluster.scheduler.api.service.clickhouse.clickhouse.ClickhouseService;
import com.bilibili.cluster.scheduler.common.Constants;
import com.bilibili.cluster.scheduler.common.dto.bmr.config.ConfigDetailData;
import com.bilibili.cluster.scheduler.common.dto.bmr.experiment.ExperimentType;
import com.bilibili.cluster.scheduler.common.dto.bmr.metadata.MetadataClusterData;
import com.bilibili.cluster.scheduler.common.dto.bmr.metadata.MetadataComponentData;
import com.bilibili.cluster.scheduler.common.dto.bmr.resource.ComponentNodeDetail;
import com.bilibili.cluster.scheduler.common.dto.clickhouse.clickhouse.PodTemplateDTO;
import com.bilibili.cluster.scheduler.common.dto.flow.req.DeployOneFlowReq;
import com.bilibili.cluster.scheduler.common.dto.hbo.pararms.HboJobParamsUpdateFlowExtParams;
import com.bilibili.cluster.scheduler.common.dto.hdfs.nnproxy.parms.ComponentConfInfo;
import com.bilibili.cluster.scheduler.common.dto.hdfs.nnproxy.parms.NNProxyDeployFlowExtParams;
import com.bilibili.cluster.scheduler.common.dto.parameters.dto.flow.metric.MetricExtParams;
import com.bilibili.cluster.scheduler.common.dto.presto.experiment.TrinoClusterInfo;
import com.bilibili.cluster.scheduler.common.dto.presto.experiment.TrinoExperimentExtFlowParams;
import com.bilibili.cluster.scheduler.common.dto.presto.scaler.PrestoFastScalerExtFlowParams;
import com.bilibili.cluster.scheduler.common.dto.presto.tide.PrestoTideExtFlowParams;
import com.bilibili.cluster.scheduler.common.dto.presto.tide.PrestoToPrestoTideExtFlowParams;
import com.bilibili.cluster.scheduler.common.dto.spark.client.SparkClientDeployExtParams;
import com.bilibili.cluster.scheduler.common.dto.spark.client.SparkClientDeployType;
import com.bilibili.cluster.scheduler.common.dto.bmr.experiment.ExperimentJobType;
import com.bilibili.cluster.scheduler.common.dto.spark.params.SparkDeployFlowExtParams;
import com.bilibili.cluster.scheduler.common.dto.spark.params.SparkDeployType;
import com.bilibili.cluster.scheduler.common.dto.spark.params.SparkExperimentFlowExtParams;
import com.bilibili.cluster.scheduler.common.dto.spark.params.SparkVersionLockExtParams;
import com.bilibili.cluster.scheduler.common.dto.spark.periphery.SparkPeripheryComponent;
import com.bilibili.cluster.scheduler.common.dto.spark.periphery.SparkPeripheryComponentDeployFlowExtParams;
import com.bilibili.cluster.scheduler.common.dto.tide.flow.YarnTideExtFlowParams;
import com.bilibili.cluster.scheduler.common.dto.tide.type.DynamicScalingStrategy;
import com.bilibili.cluster.scheduler.common.enums.flow.FlowDeployPackageType;
import com.bilibili.cluster.scheduler.common.enums.flow.FlowDeployType;
import com.bilibili.cluster.scheduler.common.enums.flow.FlowGroupTypeEnum;
import com.bilibili.cluster.scheduler.common.enums.flow.FlowReleaseScopeType;
import com.bilibili.cluster.scheduler.common.enums.flow.FlowUrgencyType;
import com.bilibili.cluster.scheduler.common.enums.flow.SubDeployType;
import com.bilibili.cluster.scheduler.common.enums.resourceV2.TideClusterType;
import com.bilibili.cluster.scheduler.common.utils.NumberUtils;
import com.google.common.base.Preconditions;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
public class FlowParamsValidatorImpl implements FlowParamsValidator {

    @Resource
    BmrMetadataService bmrMetadataService;

    @Resource
    BmrResourceService bmrResourceService;

    @Resource
    BmrConfigService bmrConfigService;

    @Resource
    ClickhouseService clickhouseService;

    @Override
    public void validate(DeployOneFlowReq req) throws Exception {
        if (req.getGroupType() == null) {
            req.setGroupType(FlowGroupTypeEnum.RANDOM_GROUP);
        }

        int parallelism = req.getParallelism().intValue();
        Preconditions.checkState(parallelism <= 500, "并发度超过500");

//        Preconditions.checkState(NumberUtils.isPositiveLong(req.getClusterId()), "clusterId is illegal");
//        Preconditions.checkState(NumberUtils.isPositiveLong(req.getComponentId()), "clusterId is illegal");
        Preconditions.checkState(!StringUtils.isBlank(req.getUserName()), "op user info is blank");

        String extParams = req.getExtParams();

        FlowDeployType deployType = req.getDeployType();
        switch (deployType) {
            case MODIFY_MONITOR_OBJECT:
                checkMonitorDeployExtParams(extParams);
                break;
            case SPARK_EXPERIMENT:
                checkSparkExperimentFlowExtParams(req);
                break;
            case SPARK_DEPLOY:
            case SPARK_DEPLOY_ROLLBACK:
                checkSparkDeployerFlowParams(req);
                break;
            case SPARK_VERSION_LOCK:
            case SPARK_VERSION_RELEASE:
                checkSparkVersionLockFlowParams(req);
                break;
            case SPARK_CLIENT_PACKAGE_DEPLOY:
                checkSparkClientPackageDeployFlowParams(req);
                break;
            case PRESTO_TIDE_ON:
            case PRESTO_TIDE_OFF:
                checkPrestoTideFlowParams(req);
                break;
            case PRESTO_TO_PRESTO_TIDE_ON:
            case PRESTO_TO_PRESTO_TIDE_OFF:
                checkPrestoToPrestoTideFlowParams(req);
                break;
            case CK_TIDE_OFF:
            case CK_TIDE_ON:
                checkCkTideFlowParams(req);
                break;
            case HBO_JOB_PARAM_RULE_UPDATE:
                checkHboJobParamsUpdate(req);
                break;
            case HBO_JOB_PARAM_RULE_DELETE:
                break;
            case K8S_CAPACITY_EXPANSION:
            case K8S_ITERATION_RELEASE:
                checkContainerDeploy(req);
                break;
            case SPARK_PERIPHERY_COMPONENT_DEPLOY:
            case SPARK_PERIPHERY_COMPONENT_ROLLBACK:
                checkSparkPeripheryComponentDeploy(req);
                break;
            case SPARK_PERIPHERY_COMPONENT_LOCK:
            case SPARK_PERIPHERY_COMPONENT_RELEASE:
                checkSparkPeripheryComponentLockParams(req);
                break;
            case NNPROXY_DEPLOY:
                checkNNProxyDeployParams(req);
                break;
            case NNPROXY_RESTART:
                checkNNProxyRestartParams(req);
                break;
            case PRESTO_FAST_SHRINK:
            case PRESTO_FAST_EXPANSION:
                checkPrestoFastScaler(req);
                break;
            case YARN_TIDE_SHRINK:
            case YARN_TIDE_EXPANSION:
                checkYarnTideScaler(req);
                break;
            case OFF_LINE_EVICTION:
                checkOffLineEvictionParams(req);
                break;
            case CAPACITY_EXPANSION:
                checkCapacityExpansionParams(req);
                break;
            case ITERATION_RELEASE:
                checkIterationReleaseParams(req);
                break;
            case TRINO_EXPERIMENT:
                checkTrinoExperimentFlowExtParams(req);
                break;

        }

    }

    private void checkIterationReleaseParams(DeployOneFlowReq req) {
        Long componentId = req.getComponentId();
        String componentName = req.getComponentName();
        if (NumberUtils.isPositiveLong(componentId)) {
            MetadataComponentData componentData = bmrMetadataService.queryComponentByComponentId(componentId);
            if (Objects.isNull(componentData)) {
                throw new IllegalArgumentException("component is not exist, component id is " + componentId);
            }
            componentName = componentData.getComponentName();
        }

        String clusterName = req.getClusterName();

        Long clusterId = req.getClusterId();
        if (NumberUtils.isPositiveLong(clusterId)) {
            MetadataClusterData clusterData = bmrMetadataService.queryClusterDetail(clusterId);
            if (Objects.isNull(clusterData)) {
                throw new IllegalArgumentException("cluster is not exist, cluster id is " + clusterId);
            }
            clusterName = clusterData.getClusterName();
        }

        if (Constants.ZK_COMPONENT.equals(componentName)) {
            Long configId = req.getConfigId();
            if (!NumberUtils.isPositiveLong(configId)){
                throw new IllegalArgumentException("configId is not exist");
            }

            ConfigDetailData configDetailData = bmrConfigService.queryConfigDetailById(configId);

            if (Objects.isNull(configDetailData)) {
                throw new IllegalArgumentException("config is not exist, config id is " + configId);
            }

        }
    }

    private void checkCapacityExpansionParams(DeployOneFlowReq req) {
        Long componentId = req.getComponentId();
        String componentName = req.getComponentName();
        if (NumberUtils.isPositiveLong(componentId)) {
            MetadataComponentData componentData = bmrMetadataService.queryComponentByComponentId(componentId);
            if (Objects.isNull(componentData)) {
                throw new IllegalArgumentException("component is not exist, component id is " + componentId);
            }
            componentName = componentData.getComponentName();
        }

        String clusterName = req.getClusterName();

        Long clusterId = req.getClusterId();
        if (NumberUtils.isPositiveLong(clusterId)) {
            MetadataClusterData clusterData = bmrMetadataService.queryClusterDetail(clusterId);
            if (Objects.isNull(clusterData)) {
                throw new IllegalArgumentException("cluster is not exist, cluster id is " + clusterId);
            }
            clusterName = clusterData.getClusterName();
        }

        if (Constants.ZK_COMPONENT.equals(componentName)) {
            List<String> nodeList = req.getNodeList();
            List<ComponentNodeDetail> componentNodeList = bmrResourceService.queryComponentNodeList(clusterId, componentId);
            List<String> afterHostList = componentNodeList.stream()
                    .filter(node -> Constants.RUNNING_STATUS.equals(node.getApplicationState()) && !nodeList.contains(node.getHostName()))
                    .map(ComponentNodeDetail::getHostName)
                    .collect(Collectors.toList());
            afterHostList.addAll(nodeList);

            if (afterHostList.size() % 2 == 0) {
                throw new IllegalArgumentException("after expansion zk node size must be odd, after host list is " + afterHostList);
            }
        }
    }

    private void checkOffLineEvictionParams(DeployOneFlowReq req) {
        Long componentId = req.getComponentId();
        String componentName = req.getComponentName();
        if (NumberUtils.isPositiveLong(componentId)) {
            MetadataComponentData componentData = bmrMetadataService.queryComponentByComponentId(componentId);
            if (Objects.isNull(componentData)) {
                throw new IllegalArgumentException("component is not exist, component id is " + componentId);
            }
            componentName = componentData.getComponentName();
        }

        String clusterName = req.getClusterName();

        Long clusterId = req.getClusterId();
        if (NumberUtils.isPositiveLong(clusterId)) {
            MetadataClusterData clusterData = bmrMetadataService.queryClusterDetail(clusterId);
            if (Objects.isNull(clusterData)) {
                throw new IllegalArgumentException("cluster is not exist, cluster id is " + clusterId);
            }
            clusterName = clusterData.getClusterName();
        }

        if (Constants.ZK_COMPONENT.equals(componentName)) {
            List<String> nodeList = req.getNodeList();
            List<ComponentNodeDetail> componentNodeList = bmrResourceService.queryComponentNodeList(clusterId, componentId);
            List<String> afterHostList = componentNodeList.stream()
                    .filter(node -> Constants.RUNNING_STATUS.equals(node.getApplicationState()) && !nodeList.contains(node.getHostName()))
                    .map(ComponentNodeDetail::getHostName)
                    .collect(Collectors.toList());
            if (afterHostList.size() < 3) {
                throw new IllegalArgumentException("after eviction zk node size must be greater than 3, after host list is " + afterHostList);
            }

            if (afterHostList.size() % 2 == 0) {
                throw new IllegalArgumentException("after eviction zk node size must be odd, after host list is " + afterHostList);
            }
        }
    }


    private void checkPrestoToPrestoTideFlowParams(DeployOneFlowReq req) {
        final String extParams = req.getExtParams();
        PrestoToPrestoTideExtFlowParams prestoTideExtFlowParams = JSONUtil.toBean(extParams, PrestoToPrestoTideExtFlowParams.class);
        int sinkCurrentPod = prestoTideExtFlowParams.getSinkCurrentPod();
        int sinkRemainPod = prestoTideExtFlowParams.getSinkRemainPod();
        int sourceCurrentPod = prestoTideExtFlowParams.getSourceCurrentPod();
        int sourceRemainPod = prestoTideExtFlowParams.getSourceRemainPod();

        Preconditions.checkState(sinkCurrentPod > 0, "require sink currentPod > 0");
        Preconditions.checkState(sinkRemainPod > 0, "require sink remainPod > 0");
        Preconditions.checkState(sourceCurrentPod > 0, "require source remainPod > 0");
        Preconditions.checkState(sourceRemainPod > 0, "require source remainPod > 0");

        Preconditions.checkState(sinkCurrentPod > sinkRemainPod, "require sink currentPod > remainPod");
        Preconditions.checkState(sourceCurrentPod > sourceRemainPod, "require source currentPod > remainPod");

//        todo:后续二阶段完成
//        final long yarnClusterId = prestoTideExtFlowParams.getYarnClusterId();
//        Preconditions.checkState(yarnClusterId > 0, "require yarnClusterId > 0");
//
//        final String appId = prestoTideExtFlowParams.getAppId();
//        Preconditions.checkState(appId.contains("trino"), "appId require trino cluster");

        long sourceClusterId = prestoTideExtFlowParams.getSourceClusterId();
        long sourceComponentId = prestoTideExtFlowParams.getSourceComponentId();
        Preconditions.checkState(sourceClusterId > 0, "source presto cluster id is require");
        Preconditions.checkState(sourceComponentId > 0, "source presto component id is require");

        long sinkClusterId = prestoTideExtFlowParams.getSinkClusterId();
        long sinkComponentId = prestoTideExtFlowParams.getSinkComponentId();
        Preconditions.checkState(sinkClusterId > 0, "sink presto cluster id is require");
        Preconditions.checkState(sinkComponentId > 0, "sink presto component id is require");
        req.setParallelism(1);
    }

    private void checkCkTideFlowParams(DeployOneFlowReq req) {
        Long clusterId = req.getClusterId();
        MetadataClusterData clusterData = bmrMetadataService.queryClusterDetail(clusterId);
        if (Objects.isNull(clusterData)) {
            throw new IllegalArgumentException("集群不存在,集群id为" + clusterId);
        }

        Long componentId = req.getComponentId();
        MetadataComponentData componentData = bmrMetadataService.queryComponentByComponentId(componentId);

        if (Objects.isNull(componentData)) {
            throw new IllegalArgumentException("组件不存在,组件id为" + componentId);
        }

        List<PodTemplateDTO> podTemplateList = clickhouseService.queryPodTemplateListByClusterId(clusterId);
        podTemplateList.stream()
                .filter(podTemplate -> Constants.CK_STABLE_TEMPLATE.equals(podTemplate.getTemplateName()))
                .findAny()
                .orElseThrow(() -> new IllegalArgumentException("集群" + clusterId + "不存在stable pod template"));
    }

    private void checkContainerDeploy(DeployOneFlowReq req) {
        Long clusterId = req.getClusterId();
        MetadataClusterData clusterData = bmrMetadataService.queryClusterDetail(clusterId);
        if (Objects.isNull(clusterData)) {
            throw new IllegalArgumentException("集群不存在,集群id为" + clusterId);
        }

        Long componentId = req.getComponentId();
        MetadataComponentData componentData = bmrMetadataService.queryComponentByComponentId(componentId);

        if (Objects.isNull(componentData)) {
            throw new IllegalArgumentException("组件不存在,组件id为" + componentId);
        }
    }

    private void checkHboJobParamsUpdate(DeployOneFlowReq req) {
        String extParams = req.getExtParams();
        Preconditions.checkNotNull(extParams,
                "deploy of HBO_JOB_PARAM_RULE_UPDATE but find extParams is null");
        HboJobParamsUpdateFlowExtParams hboJobParamsUpdateFlowExtParams = JSONUtil.toBean(extParams, HboJobParamsUpdateFlowExtParams.class);
        Map<String, String> addParamsMap = hboJobParamsUpdateFlowExtParams.getAddParamsMap();
        Map<String, String> removeParamsMap = hboJobParamsUpdateFlowExtParams.getRemoveParamsMap();
        if (Objects.isNull(addParamsMap) && Objects.isNull(removeParamsMap)) {
            throw new IllegalArgumentException("新增和移除参数不能同时为空");
        }

        if (!Objects.isNull(addParamsMap) && !Objects.isNull(removeParamsMap)) {
            Set<String> addKeySet = addParamsMap.keySet();
            Set<String> removeKeySet = removeParamsMap.keySet();
            List<String> duplicateKeyList = addKeySet.stream()
                    .filter(removeKeySet::contains)
                    .collect(Collectors.toList());
            Assert.isTrue(org.springframework.util.CollectionUtils.isEmpty(duplicateKeyList), "新增和移除参数的key不能重复,重复的key为" + duplicateKeyList);
        }
    }

    private void checkMonitorDeployExtParams(String extParams) {
        Preconditions.checkNotNull(extParams,
                "deploy of MODIFY_MONITOR_OBJECT but find extParams is null");
        MetricExtParams metricExtParams = JSONUtil.toBean(extParams, MetricExtParams.class);
        Preconditions.checkNotNull(metricExtParams.getModifyType(), "MonitorExtParams of modifyType is null");
    }

    private void checkSparkExperimentFlowExtParams(DeployOneFlowReq req) {
        String extParams = req.getExtParams();
        Preconditions.checkNotNull(extParams,
                "deploy of SPARK_EXPERIMENT but find extParams is null");
        SparkExperimentFlowExtParams experimentFlowExtParams = JSONUtil.toBean(extParams, SparkExperimentFlowExtParams.class);

        Preconditions.checkState(experimentFlowExtParams.getInstanceId() > 0, "实验任务实例id不存在");
        final String platformA = experimentFlowExtParams.getPlatformA();
        Preconditions.checkState(!StringUtils.isBlank(platformA), "platformA is blank");

        final String imageA = experimentFlowExtParams.getImageA();
        Preconditions.checkState(!StringUtils.isBlank(imageA), "imageA is blank");

        final ExperimentType experimentType = experimentFlowExtParams.getExperimentType();
        Preconditions.checkNotNull(experimentType, "experimentType is null, support of: " + Arrays.asList(ExperimentType.values()));

        switch (experimentType) {
            case PERFORMANCE_TEST:
                break;
            case COMPARATIVE_TASK:
                final String platformB = experimentFlowExtParams.getPlatformB();
                Preconditions.checkState(!StringUtils.isBlank(platformB), "platformB is blank");
                final String imageB = experimentFlowExtParams.getImageB();
                Preconditions.checkState(!StringUtils.isBlank(imageB), "imageB is blank");
                break;
        }

        ExperimentJobType jobType = experimentFlowExtParams.getJobType();
        Preconditions.checkNotNull(jobType, "jobType is null, support of: " + Arrays.asList(ExperimentJobType.values()));

        if (ExperimentJobType.TEST_JOB == jobType) {
            Preconditions.checkState(experimentFlowExtParams.getTestSetVersionId() > 0, "测试集版本不存在");
        } else {
            List<String> nodeList = req.getNodeList();
            Preconditions.checkState(!CollectionUtils.isEmpty(nodeList), "compass任务列表为空");
        }
    }

    private void checkSparkDeployerFlowParams(DeployOneFlowReq req) {
        final String releaseScopeTypeValue = req.getReleaseScopeType();
        Preconditions.checkState(!StringUtils.isBlank(releaseScopeTypeValue), "releaseScopeType is blank");
        FlowReleaseScopeType releaseScopeType = FlowReleaseScopeType.valueOf(releaseScopeTypeValue);

        final String extParams = req.getExtParams();
        final SparkDeployFlowExtParams sparkDeployFlowExtParams = JSONUtil.toBean(extParams, SparkDeployFlowExtParams.class);

        final String targetSparkVersion = sparkDeployFlowExtParams.getTargetSparkVersion();
        Preconditions.checkState(!StringUtils.isBlank(targetSparkVersion), "target spark version is blank.");
        final List<Integer> percentStageList = sparkDeployFlowExtParams.getPercentStageList();

        final SparkDeployType sparkDeployType = sparkDeployFlowExtParams.getSparkDeployType();
        Preconditions.checkNotNull(sparkDeployType, "spark发布场景类型为空");

        final FlowDeployType deployType = req.getDeployType();
        final List<String> jobIdList = req.getNodeList();

        if (deployType == FlowDeployType.SPARK_DEPLOY) {
            final String majorSparkVersion = sparkDeployFlowExtParams.getMajorSparkVersion();
            Preconditions.checkState(!StringUtils.isBlank(majorSparkVersion), "spark大版本为空");
            if (sparkDeployType == SparkDeployType.NORMAL) {
                Preconditions.checkState(!CollectionUtils.isEmpty(percentStageList), "percentStageList is empty");
                Integer prePercent = 0;
                for (Integer curPercent : percentStageList) {
                    Preconditions.checkState(curPercent.intValue() > prePercent.intValue(), "curPercent require big than prePercent: " + percentStageList);
                    prePercent = curPercent;
                }
                Preconditions.checkState(prePercent.intValue() == 100, "last stage percent require is 100");
            }

            switch (releaseScopeType) {
                case GRAY_RELEASE:
                    Preconditions.checkState(!CollectionUtils.isEmpty(jobIdList), "spark deploy of [GRAY_RELEASE] require jobId list is not empty");
                    break;
                case FULL_RELEASE:
                    Preconditions.checkState(CollectionUtils.isEmpty(jobIdList), "spark deploy of [FULL_RELEASE] require jobId list is empty");
                    break;
                default:
                    throw new IllegalArgumentException("spark deploy not support of releaseScopeType: " + releaseScopeTypeValue);
            }
        } else {
            final String originalSparkVersion = sparkDeployFlowExtParams.getOriginalSparkVersion();
            Preconditions.checkState(!StringUtils.isBlank(originalSparkVersion), "待回滚的spark版本为空");
            Preconditions.checkState(CollectionUtils.isEmpty(jobIdList), "spark deploy rollback require jobId list is empty");
        }
    }

    private void checkSparkVersionLockFlowParams(DeployOneFlowReq req) {
        final List<String> jobIdList = req.getNodeList();
        Preconditions.checkState(!CollectionUtils.isEmpty(jobIdList), "spark version lock or release require jobId list is not empty");
        final String extParams = req.getExtParams();
        SparkVersionLockExtParams sparkVersionLockExtParams = JSONUtil.toBean(extParams, SparkVersionLockExtParams.class);

        List<String> approverList = sparkVersionLockExtParams.getApproverList();
        Preconditions.checkState(!CollectionUtils.isEmpty(approverList),
                "spark version lock or release ext params approver list is empty");
    }

    private void checkSparkClientPackageDeployFlowParams(DeployOneFlowReq req) {
        final FlowDeployType deployType = req.getDeployType();
        final List<String> nodeList = req.getNodeList();
        final String releaseScopeType = req.getReleaseScopeType();
        Preconditions.checkState(!StringUtils.isBlank(releaseScopeType), "releaseScopeType is require");
        final FlowReleaseScopeType scopeType = FlowReleaseScopeType.valueOf(releaseScopeType);

        if (scopeType != FlowReleaseScopeType.FULL_RELEASE) {
            Preconditions.checkState(!CollectionUtils.isEmpty(nodeList), "node list is empty");
        }
        final String extParams = req.getExtParams();
        SparkClientDeployExtParams sparkClientDeployExtParams = JSONUtil.toBean(extParams, SparkClientDeployExtParams.class);
        final SparkClientDeployType packDeployType = sparkClientDeployExtParams.getPackDeployType();
        Preconditions.checkNotNull(packDeployType, "packDeployType is null");
        final List<Long> packIdList = sparkClientDeployExtParams.getPackIdList();

        switch (packDeployType) {
            case ADD_NEWLY_HOSTS:
            case REMOVE_USELESS_VERSION:
            case ADD_NEWLY_VERSION:
                Preconditions.checkState(!CollectionUtils.isEmpty(packIdList), "packIdList is empty");
                break;
        }

    }

    private void checkPrestoTideFlowParams(DeployOneFlowReq req) {
        final String extParams = req.getExtParams();
        PrestoTideExtFlowParams prestoTideExtFlowParams = JSONUtil.toBean(extParams, PrestoTideExtFlowParams.class);
        final int currentPod = prestoTideExtFlowParams.getCurrentPod();
        final int remainPod = prestoTideExtFlowParams.getRemainPod();

        Preconditions.checkState(currentPod > 0, "require currentPod > 0");
        Preconditions.checkState(remainPod > 0, "require remainPod > 0");
        Preconditions.checkState(currentPod > remainPod, "require currentPod > remainPod");
        final long yarnClusterId = prestoTideExtFlowParams.getYarnClusterId();
        Preconditions.checkState(yarnClusterId > 0, "require yarnClusterId > 0");

        final String appId = prestoTideExtFlowParams.getAppId();
        Preconditions.checkState(appId.contains("trino"), "appId require trino cluster");

        final Long clusterId = req.getClusterId();
        final Long componentId = req.getComponentId();
        Preconditions.checkState(clusterId > 0, "presto cluster id is require");
        Preconditions.checkState(componentId > 0, "presto component id is require");
        req.setParallelism(1);
    }

    private void checkSparkPeripheryComponentDeploy(DeployOneFlowReq req) {
        final String releaseScopeTypeValue = req.getReleaseScopeType();
        Preconditions.checkState(!StringUtils.isBlank(releaseScopeTypeValue), "releaseScopeType is blank");
        FlowReleaseScopeType releaseScopeType = FlowReleaseScopeType.valueOf(releaseScopeTypeValue);
        final String extParams = req.getExtParams();

        SparkPeripheryComponentDeployFlowExtParams deployExtParams = JSONUtil.toBean(extParams, SparkPeripheryComponentDeployFlowExtParams.class);
        final SparkPeripheryComponent peripheryComponent = deployExtParams.getPeripheryComponent();
        Preconditions.checkNotNull(peripheryComponent, "peripheryComponent params is null");

        final String targetVersion = deployExtParams.getTargetVersion();
        Preconditions.checkState(StringUtils.isNotEmpty(targetVersion), "spark periphery component deploy target version is blank");
        List<String> jobIdList = req.getNodeList();
        switch (releaseScopeType) {
            case GRAY_RELEASE:
                Preconditions.checkState(!CollectionUtils.isEmpty(jobIdList),
                        "spark periphery component deploy of [GRAY_RELEASE] require jobId list is not empty");
                break;
            case FULL_RELEASE:
                Preconditions.checkState(CollectionUtils.isEmpty(jobIdList),
                        "spark periphery component deploy of [FULL_RELEASE] require jobId list is empty");
                break;
            default:
                throw new IllegalArgumentException("spark deploy not support of releaseScopeType: " + releaseScopeTypeValue);
        }
        final FlowDeployType deployType = req.getDeployType();
        if (FlowDeployType.SPARK_PERIPHERY_COMPONENT_ROLLBACK == deployType) {
            final String originalVersion = deployExtParams.getOriginalVersion();
            Preconditions.checkState(StringUtils.isNotEmpty(originalVersion), "spark component rollback leak [originalVersion] params");
        }
    }

    private void checkSparkPeripheryComponentLockParams(DeployOneFlowReq req) {
        final String releaseScopeTypeValue = req.getReleaseScopeType();
        Preconditions.checkState(!StringUtils.isBlank(releaseScopeTypeValue), "releaseScopeType is blank");
        FlowReleaseScopeType releaseScopeType = FlowReleaseScopeType.valueOf(releaseScopeTypeValue);
        final String extParams = req.getExtParams();

        Preconditions.checkState(releaseScopeType == FlowReleaseScopeType.GRAY_RELEASE, "spark component lock or release require scope is [GRAY_RELEASE]");

        SparkPeripheryComponentDeployFlowExtParams deployExtParams = JSONUtil.toBean(extParams, SparkPeripheryComponentDeployFlowExtParams.class);
        final SparkPeripheryComponent peripheryComponent = deployExtParams.getPeripheryComponent();
        Preconditions.checkNotNull(peripheryComponent, "peripheryComponent params is null");

        List<String> jobIdList = req.getNodeList();
        Preconditions.checkState(!CollectionUtils.isEmpty(jobIdList),
                "spark periphery component lock/release of [GRAY_RELEASE] require jobId list is not empty");
    }


    private void checkNNProxyDeployParams(DeployOneFlowReq req) {
        final Long clusterId = req.getClusterId();
        Preconditions.checkState(NumberUtils.isPositiveLong(clusterId), "clusterId not exists");

        final String extParams = req.getExtParams();
        final NNProxyDeployFlowExtParams flowExtParams = JSONUtil.toBean(extParams, NNProxyDeployFlowExtParams.class);
        final SubDeployType subDeployType = flowExtParams.getSubDeployType();
        final String releaseScopeType = req.getReleaseScopeType();
        Preconditions.checkState(StringUtils.isNotEmpty(releaseScopeType), "releaseScopeType is blank");
        FlowReleaseScopeType scopeType = FlowReleaseScopeType.valueOf(releaseScopeType);
        final String packageType = req.getDeployPackageType();
        Preconditions.checkState(StringUtils.isNotEmpty(packageType), "deployPackageType is blank");
        FlowDeployPackageType deployPackageType = FlowDeployPackageType.valueOf(packageType);
        final List<String> nodeList = req.getNodeList();


        switch (subDeployType) {
            case ITERATION_RELEASE:
                if (scopeType != FlowReleaseScopeType.FULL_RELEASE && scopeType != FlowReleaseScopeType.GRAY_RELEASE) {
                    throw new IllegalArgumentException("ITERATION require scopeType is 'FULL_RELEASE' or 'GRAY_RELEASE', now is: " + scopeType);
                }

                if (flowExtParams.getUrgencyType() == FlowUrgencyType.NORMAL) {
                    Preconditions.checkState(req.getParallelism() == 1, "ITERATION_RELEASE parallelism require '1'");
                }

                // 全量发布，节点动态获取，发布包类型校验
                if (scopeType == FlowReleaseScopeType.FULL_RELEASE) {
                    if (deployPackageType == FlowDeployPackageType.ALL_PACKAGE ||
                            deployPackageType == FlowDeployPackageType.SERVICE_PACKAGE) {
                        final Long packageId = req.getPackageId();
                        Preconditions.checkState(NumberUtils.isPositiveLong(packageId), "packageId not exists");
                    }
                    List<ComponentConfInfo> confInfoList = flowExtParams.getConfInfoList();
                    if (deployPackageType == FlowDeployPackageType.ALL_PACKAGE ||
                            deployPackageType == FlowDeployPackageType.CONFIG_PACKAGE) {
                        Preconditions.checkState(!CollectionUtils.isEmpty(confInfoList), "ext params confInfoList is blank");
                    }
                    Preconditions.checkState(CollectionUtils.isEmpty(nodeList), "FULL_RELEASE require nodeList is empty");
                }
                // 迭代发布，节点固定， 发布包类型校验
                if (scopeType == FlowReleaseScopeType.GRAY_RELEASE) {
                    final Long componentId = req.getComponentId();
                    Preconditions.checkState(NumberUtils.isPositiveLong(componentId), "componentId not exists");
                    Preconditions.checkState(!CollectionUtils.isEmpty(nodeList), "GRAY_RELEASE but not find any nodes");

                    // 校验发布包类型
                    if (deployPackageType == FlowDeployPackageType.ALL_PACKAGE ||
                            deployPackageType == FlowDeployPackageType.SERVICE_PACKAGE) {
                        final Long packageId = req.getPackageId();
                        Preconditions.checkState(NumberUtils.isPositiveLong(packageId), "packageId not exists");
                    }
                    if (deployPackageType == FlowDeployPackageType.ALL_PACKAGE ||
                            deployPackageType == FlowDeployPackageType.CONFIG_PACKAGE) {
                        final Long configId = req.getConfigId();
                        Preconditions.checkState(NumberUtils.isPositiveLong(configId), "configId not exists");
                    }
                }
                break;
            case CAPACITY_EXPANSION:
                final Long componentId = req.getComponentId();
                Preconditions.checkState(NumberUtils.isPositiveLong(componentId), "EXPANSION, but componentId not exists");
                Preconditions.checkState(!CollectionUtils.isEmpty(nodeList), "EXPANSION but not find any nodes");
                Preconditions.checkState(deployPackageType == FlowDeployPackageType.ALL_PACKAGE,
                        "EXPANSION require deployPackageType is [ALL_PACKAGE]");
                Preconditions.checkState(NumberUtils.isPositiveLong(req.getPackageId()), "EXPANSION require packId");
                Preconditions.checkState(NumberUtils.isPositiveLong(req.getConfigId()), "EXPANSION require configId");
                break;
        }
    }


    private void checkNNProxyRestartParams(DeployOneFlowReq req) {
        final Long clusterId = req.getClusterId();
        Preconditions.checkState(NumberUtils.isPositiveLong(clusterId), "clusterId not exists");
        final String releaseScopeType = req.getReleaseScopeType();
        Preconditions.checkState(StringUtils.isNotEmpty(releaseScopeType), "releaseScopeType is blank");
        FlowReleaseScopeType scopeType = FlowReleaseScopeType.valueOf(releaseScopeType);
        final String packageType = req.getDeployPackageType();
        Preconditions.checkState(StringUtils.isNotEmpty(packageType), "deployPackageType is blank");
        FlowDeployPackageType deployPackageType = FlowDeployPackageType.valueOf(packageType);
        Preconditions.checkState(deployPackageType.equals(FlowDeployPackageType.NONE_PACKAGE), "deployPackageType is illegal: " + deployPackageType);

        final List<String> nodeList = req.getNodeList();
        if (scopeType.equals(FlowReleaseScopeType.FULL_RELEASE)) {
            Preconditions.checkState(CollectionUtils.isEmpty(nodeList), "FULL_RELEASE require nodeList is empty");
        }
        final Long componentId = req.getComponentId();
        if (scopeType.equals(FlowReleaseScopeType.GRAY_RELEASE)) {
            Preconditions.checkState(NumberUtils.isPositiveLong(componentId), "componentId not exists");
            Preconditions.checkState(!CollectionUtils.isEmpty(nodeList), "GRAY_RELEASE but not find any nodes");
        }
    }

    private void checkYarnTideScaler(DeployOneFlowReq req) {
        final Long clusterId = req.getClusterId();
        Preconditions.checkState(NumberUtils.isPositiveLong(clusterId), "clusterId not exists");
        final String extParams = req.getExtParams();
        Preconditions.checkState(StringUtils.isNotBlank(extParams), "extParams is blank");

        final YarnTideExtFlowParams yarnTideExtFlowParams = JSONUtil.toBean(extParams, YarnTideExtFlowParams.class);
        Preconditions.checkState(yarnTideExtFlowParams.getExpectedCount() > 0, "expectedCount is error");
        final TideClusterType clusterType = yarnTideExtFlowParams.getClusterType();
        Preconditions.checkNotNull(clusterType, "clusterType is null");
    }

    private void checkPrestoFastScaler(DeployOneFlowReq req) {
        final Long clusterId = req.getClusterId();
        Preconditions.checkState(NumberUtils.isPositiveLong(clusterId), "clusterId not exists");
        final Long componentId = req.getComponentId();
        Preconditions.checkState(NumberUtils.isPositiveLong(componentId), "componentId not exists");

        final String extParams = req.getExtParams();
        Preconditions.checkState(StringUtils.isNotBlank(extParams), "extParams is blank");
        final PrestoFastScalerExtFlowParams prestoFastScalerExtFlowParams = JSONUtil.toBean(extParams, PrestoFastScalerExtFlowParams.class);
        Preconditions.checkState(prestoFastScalerExtFlowParams.getHighPodNum() > 0, "highPodNum is error");
        Preconditions.checkState(prestoFastScalerExtFlowParams.getLowPodNum() > 0, "lowPodNum is error");

        final DynamicScalingStrategy dynamicScalingStrategy = prestoFastScalerExtFlowParams.getDynamicScalingStrategy();
        Preconditions.checkNotNull(dynamicScalingStrategy, "dynamicScalingStrategy is null");

    }

    private void checkTrinoExperimentFlowExtParams(DeployOneFlowReq req) {
        final List<String> jobIdList = req.getNodeList();
        Preconditions.checkState(!CollectionUtils.isEmpty(jobIdList), "node list is empty");

        final String extParams = req.getExtParams();
        TrinoExperimentExtFlowParams extFlowParams = JSONUtil.toBean(extParams, TrinoExperimentExtFlowParams.class);
        final ExperimentType experimentType = extFlowParams.getExperimentType();
        Preconditions.checkNotNull(experimentType, "experimentType is null");
        final String imageA = extFlowParams.getImageA();
        Preconditions.checkState(!StringUtils.isBlank(imageA), "imageA is blank");

        final String aRunTimeConf = extFlowParams.getARunTimeConf();
        checkTrinoClusterInfo(aRunTimeConf);

        final long versionId = extFlowParams.getTestSetVersionId();
        Preconditions.checkState(NumberUtils.isPositiveLong(versionId), "test set version id is illegal");
        final long instanceId = extFlowParams.getInstanceId();
        Preconditions.checkState(NumberUtils.isPositiveLong(instanceId), "experiment instance id is illegal");
        final String platformA = extFlowParams.getPlatformA();
        Preconditions.checkState(!StringUtils.isBlank(platformA), "platformA is blank");

        if (experimentType == ExperimentType.COMPARATIVE_TASK) {
            String platformB = extFlowParams.getPlatformB();
            Preconditions.checkState(!StringUtils.isBlank(platformB), "platformB is blank");
            final String imageB = extFlowParams.getImageB();
            Preconditions.checkState(!StringUtils.isBlank(imageB), "imageB is blank");
            final String bRunTimeConf = extFlowParams.getBRunTimeConf();
            checkTrinoClusterInfo(bRunTimeConf);
        }

    }

    private void checkTrinoClusterInfo(String runtimeConf) {
        Preconditions.checkState(!StringUtils.isBlank(runtimeConf), "runTimeConf is blank");
        final TrinoClusterInfo trinoClusterInfo = JSONUtil.toBean(runtimeConf, TrinoClusterInfo.class);
        Preconditions.checkNotNull(trinoClusterInfo, "trinoClusterInfo is null");
        final boolean rebuildCluster = trinoClusterInfo.isRebuildCluster();
        if (rebuildCluster) {
            Preconditions.checkState(NumberUtils.isPositiveLong(trinoClusterInfo.getClusterId()), "clusterId is illegal");
            Preconditions.checkState(NumberUtils.isPositiveLong(trinoClusterInfo.getComponentId()), "componentId is illegal");
            Preconditions.checkState(NumberUtils.isPositiveLong(trinoClusterInfo.getConfigId()), "configId is illegal");
        }
    }

}
