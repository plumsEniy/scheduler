package com.bilibili.cluster.scheduler.api.service.scheduler.spark.client;

import cn.hutool.json.JSONUtil;
import com.bilibili.cluster.scheduler.api.event.analyzer.PipelineParameter;
import com.bilibili.cluster.scheduler.api.event.factory.FactoryDiscoveryUtils;
import com.bilibili.cluster.scheduler.api.event.factory.PipelineFactory;
import com.bilibili.cluster.scheduler.api.service.bmr.metadata.BmrMetadataServiceImpl;
import com.bilibili.cluster.scheduler.common.Constants;
import com.bilibili.cluster.scheduler.common.dto.bmr.metadata.InstallationPackage;
import com.bilibili.cluster.scheduler.common.dto.flow.prop.BaseFlowExtPropDTO;
import com.bilibili.cluster.scheduler.common.dto.flow.req.DeployOneFlowReq;
import com.bilibili.cluster.scheduler.common.dto.spark.client.SparkClientDeployExtParams;
import com.bilibili.cluster.scheduler.common.dto.spark.client.SparkClientDeployType;
import com.bilibili.cluster.scheduler.common.dto.spark.client.SparkClientPackInfo;
import com.bilibili.cluster.scheduler.common.dto.spark.periphery.SparkPeripheryComponent;
import com.bilibili.cluster.scheduler.common.entity.ExecutionFlowEntity;
import com.bilibili.cluster.scheduler.common.enums.flow.FlowDeployType;
import com.bilibili.cluster.scheduler.common.enums.flow.FlowReleaseScopeType;
import com.google.common.base.Preconditions;
import org.apache.avro.data.Json;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import org.springframework.beans.BeanUtils;

import java.util.ArrayList;
import java.util.List;

public class SparkClientDeployTestUnit {

    @Test
    public void getEnv() {
        List<SparkClientPackInfo> packInfos = new ArrayList<>();

        final SparkClientPackInfo oneclientInfo = new SparkClientPackInfo();
        oneclientInfo.setClientType("ONE-CLIENT");
        oneclientInfo.setPackMd5("be88b688ba40c1cd2fb804c8c87eefc6");
        oneclientInfo.setDownloadUrl("http://jssz-boss.bilibili.co/nyx-artifacts/artifact/datacenter.bmr-doctor.data-service_59128fdbbd6cac5e6820bfd906d7b615c263c116_1730376024418147-normal.tar.gz");
        oneclientInfo.setPackName("oneclient");
        packInfos.add(oneclientInfo);

        final SparkClientPackInfo sparkInfo = new SparkClientPackInfo();
        sparkInfo.setClientType("SPARK");
        sparkInfo.setDownloadUrl("http://jssz-boss.bilibili.co/nyx-artifacts/artifact/datacenter.spark.spark-build_3b0de230c430882285ab79732046a3a24f186b50_1729836677108174-normal.tar.gz");
        sparkInfo.setPackMd5("7a094f18d735945c7db864f0c7b96433");
        sparkInfo.setPackName("spark-4.0.0-preview1");
        packInfos.add(sparkInfo);

        System.out.println(JSONUtil.toJsonStr(packInfos));
    }


    @Test
    public void validReq() {
        String reqValue = "{\n" +
                "    \"flowId\": 2547,\n" +
                "    \"clusterId\": null,\n" +
                "    \"componentId\": null,\n" +
                "    \"parallelism\": 20,\n" +
                "    \"tolerance\": 1,\n" +
                "    \"nodeList\": [\n" +
                "        \"jscs-bigdata-test-39\"\n" +
                "    ],\n" +
                "    \"deployType\": \"SPARK_CLIENT_PACKAGE_DEPLOY\",\n" +
                "    \"deployPackageType\": \"SERVICE_PACKAGE\",\n" +
                "    \"releaseScopeType\": \"GRAY_RELEASE\",\n" +
                "    \"packageId\": null,\n" +
                "    \"configId\": null,\n" +
                "    \"approver\": \"nizhiqiang,liuguohui,wangchao12,xuchen,wangyuening\",\n" +
                "    \"remark\": \"one client新增主机\",\n" +
                "    \"restart\": null,\n" +
                "    \"effectiveMode\": \"IMMEDIATE_EFFECTIVE\",\n" +
                "    \"paramId\": 0,\n" +
                "    \"groupType\": null,\n" +
                "    \"userName\": \"nizhiqiang\",\n" +
                "    \"isApproval\": null,\n" +
                "    \"autoRetry\": null,\n" +
                "    \"maxRetry\": null,\n" +
                "    \"extParams\": \"{\\\"packIdList\\\":[373,360,359,351,374,372,371,370,368,367,365,357,355],\\\"packDeployType\\\":\\\"ADD_NEWLY_HOSTS\\\"}\"\n" +
                "}";

        System.out.println(reqValue);

        DeployOneFlowReq req = JSONUtil.toBean(reqValue, DeployOneFlowReq.class);


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

        System.out.println(packIdList);

        switch (packDeployType) {
            case ADD_NEWLY_HOSTS:
            case REMOVE_USELESS_VERSION:
            case ADD_NEWLY_VERSION:
                Preconditions.checkState(!CollectionUtils.isEmpty(packIdList), "packIdList is empty");
                break;
        }

        ExecutionFlowEntity executionFlowEntity = new ExecutionFlowEntity();
        BeanUtils.copyProperties(req, executionFlowEntity);

        System.out.println(JSONUtil.toJsonStr(executionFlowEntity));

        final PipelineFactory factoryByIdentifier = FactoryDiscoveryUtils.getFactoryByIdentifier(Constants.SPARK_EXPERIMENT_PIPELINE_FACTORY_IDENTIFY, PipelineFactory.class);
        System.out.println(factoryByIdentifier);
    }

    @Test
    public void testExtParams() {
        String extJson = "{\"flowExtParams\":\"{\\\"packDeployType\\\":\\\"REMOVE_USELESS_VERSION\\\",\\\"packIdList\\\":[420],\\\"packInfoList\\\":[{\\\"SPARK_CLIENT_PACK_DOWNLOAD_URL\\\":\\\"http://jssz-boss.bilibili.co/nyx-artifacts/artifact/datacenter.spark.spark4-build_06d4ac4dd2249c3c7160311e0486474a8298d29c_1732078069483952-normal.tar.gz\\\",\\\"SPARK_CLIENT_PACK_NAME\\\":\\\"v4.0.0-bilibili-0.0.1\\\",\\\"SPARK_CLIENT_PACK_MD5\\\":\\\"fe750908ea4f980f527babbfeacb8046\\\",\\\"SPARK_CLIENT_TYPE\\\":\\\"SPARK\\\"}]}\",\"nodeList\":[]}";
        final BaseFlowExtPropDTO baseFlowExtPropDTO = JSONUtil.toBean(extJson, BaseFlowExtPropDTO.class);

        final String flowExtParams = baseFlowExtPropDTO.getFlowExtParams();
        System.out.println(flowExtParams);

        SparkClientDeployExtParams extParams = JSONUtil.toBean(flowExtParams, SparkClientDeployExtParams.class);

        System.out.println(extParams);

    }

    @Test
    public void testOkHttpClient() {
        final BmrMetadataServiceImpl bmrMetadataService = new BmrMetadataServiceImpl();
        // final InstallationPackage installationPackage = bmrMetadataService.querySparkPeripheryComponentDefaultPackage(SparkPeripheryComponent.RANGER.name());
        final InstallationPackage installationPackage = bmrMetadataService.querySparkPeripheryComponentDefaultPackage(SparkPeripheryComponent.RANGER.name() + "-3");

        System.out.println(installationPackage);
    }


}
