package com.bilibili.cluster.scheduler.api.event.tide;

import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.json.JSONUtil;
import com.bilibili.cluster.scheduler.api.bean.SpringApplicationContext;
import com.bilibili.cluster.scheduler.api.event.dolphinScheduler.AbstractDolphinSchedulerEventHandler;
import com.bilibili.cluster.scheduler.api.service.GlobalService;
import com.bilibili.cluster.scheduler.api.service.flow.ExecutionFlowPropsService;
import com.bilibili.cluster.scheduler.api.service.flow.ExecutionFlowService;
import com.bilibili.cluster.scheduler.common.Constants;
import com.bilibili.cluster.scheduler.common.dto.bmr.config.ConfigDetailData;
import com.bilibili.cluster.scheduler.common.dto.bmr.config.model.ConfigGroupDo;
import com.bilibili.cluster.scheduler.common.dto.bmr.config.model.FileDownloadData;
import com.bilibili.cluster.scheduler.common.dto.bmr.metadata.MetadataClusterData;
import com.bilibili.cluster.scheduler.common.dto.bmr.metadata.MetadataComponentData;
import com.bilibili.cluster.scheduler.common.dto.bmr.metadata.MetadataPackageData;
import com.bilibili.cluster.scheduler.common.dto.bmr.resource.ComponentNodeDetail;
import com.bilibili.cluster.scheduler.common.dto.bmr.resource.model.HostAndLogicGroupInfo;
import com.bilibili.cluster.scheduler.common.dto.bmr.resource.model.ResourceHostInfo;
import com.bilibili.cluster.scheduler.common.dto.tide.flow.TideExtFlowParams;
import com.bilibili.cluster.scheduler.common.dto.yarn.RMInfoObj;
import com.bilibili.cluster.scheduler.common.entity.ExecutionFlowEntity;
import com.bilibili.cluster.scheduler.common.entity.ExecutionNodeEntity;
import com.bilibili.cluster.scheduler.common.enums.bmr.config.FileOperateType;
import com.bilibili.cluster.scheduler.common.enums.flow.FlowReleaseScopeType;
import com.bilibili.cluster.scheduler.common.enums.scheduler.DolpTaskType;
import com.bilibili.cluster.scheduler.common.event.TaskEvent;
import com.bilibili.cluster.scheduler.common.utils.NumberUtils;
import com.google.common.base.Preconditions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import javax.validation.constraints.NotNull;
import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @description: 潮汐dolphin的流程
 * @Date: 2025/3/21 10:58
 * @Author: nizhiqiang
 */

@Slf4j
public abstract class AbstractTideYarnNodeExpansionEventHandler extends AbstractDolphinSchedulerEventHandler {

    @Resource
    protected ExecutionFlowService flowService;

    @Resource
    protected ExecutionFlowPropsService flowPropsService;

    @Resource
    protected GlobalService globalService;

    /**
     * 是否跳过taskevent的检查
     *
     * @return
     */
    protected abstract boolean skipCheckEventIsRequired();

    /**
     * 获取yarn集群id
     *
     * @param flowId
     * @return
     */
    protected  long getYarnClusterId(Long flowId){
        final TideExtFlowParams tideExtFlowParams = flowPropsService.getFlowExtParamsByCache(flowId, TideExtFlowParams.class);
        return tideExtFlowParams.getYarnClusterId();
    };

    /**
     * 获取yarn主机组名称
     *
     * @return
     */
    @NotNull
    protected abstract String getYarnHostGroupName();

    @Override
    protected Map<String, Object> getDolphinExecuteEnv(TaskEvent taskEvent, List<String> hostList) {

        String yarnHostGroupName = getYarnHostGroupName();
        Preconditions.checkArgument(StringUtils.hasText(yarnHostGroupName), "yarn host group name is empty");

        Map<String, Object> instanceEnv = new LinkedHashMap<>();
        final Long flowId = taskEvent.getFlowId();
        final ExecutionFlowEntity flowEntity = flowService.getById(flowId);
        final long yarnClusterId = getYarnClusterId(flowId);

        addOrCreateHostToHostGroup(taskEvent, hostList, yarnClusterId, yarnHostGroupName);
        ThreadUtil.safeSleep(Constants.ONE_SECOND * 6);

        final List<MetadataComponentData> componentDataList = globalService.getBmrMetadataService().queryComponentListByClusterId(yarnClusterId);
        MetadataComponentData nodeManagerComponentData = null;
        MetadataComponentData resourceManagerComponentData = null;
        MetadataComponentData sparkEssWorkerComponentData = null;
        MetadataComponentData sparkEssMasterComponentData = null;
        MetadataComponentData amiyaComponentData = null;

        for (MetadataComponentData metadataComponentData : componentDataList) {
            final String componentName = metadataComponentData.getComponentName();
            switch (componentName) {
                case "ResourceManager":
                    resourceManagerComponentData = metadataComponentData;
                    break;
                case "NodeManager":
                    nodeManagerComponentData = metadataComponentData;
                    break;
                case "Amiya":
                    amiyaComponentData = metadataComponentData;
                    break;
                case "SparkEssMaster":
                    sparkEssMasterComponentData = metadataComponentData;
                    break;
                case "SparkEssWorker":
                    sparkEssWorkerComponentData = metadataComponentData;
                    break;
                default:
                    log.info("find other component name is {}", componentName);
            }
        }
        Preconditions.checkNotNull(nodeManagerComponentData, "NodeManager metadata not exist");
        Preconditions.checkNotNull(resourceManagerComponentData, "ResourceManager metadata not exist");
        Preconditions.checkNotNull(sparkEssWorkerComponentData, "SparkEssWorker metadata not exist");
        Preconditions.checkNotNull(sparkEssMasterComponentData, "SparkEssMaster metadata not exist");
        Preconditions.checkNotNull(amiyaComponentData, "Amiya metadata not exist");


        fillTideComponentAndPackageInfoToEnvMap(taskEvent, hostList, instanceEnv, flowEntity, yarnClusterId, nodeManagerComponentData, sparkEssWorkerComponentData, amiyaComponentData);

        // yarn-include 文件更新
        fillAndUpdateIncludeFileIntoEnvMap(taskEvent, hostList, instanceEnv, yarnClusterId);


        // 移除sparkEssMaster黑名单列表并泰纳加到环境变量
        fillAndUpdateSparkBlackListToEnvMap(taskEvent, hostList, yarnClusterId, sparkEssMasterComponentData);
        logPersist(taskEvent, "job env is: " + JSONUtil.toJsonStr(instanceEnv));
        return instanceEnv;
    }

    /**
     * yarn计算节点扩容,仅在阶段2执行
     *
     * @param taskEvent
     * @return
     */
    @Override
    protected boolean checkEventIsRequired(TaskEvent taskEvent) {
//        是否跳过检查
        if (skipCheckEventIsRequired()) {
            return true;
        }

        final ExecutionNodeEntity executionNode = taskEvent.getExecutionNode();
        final String execStage = executionNode.getExecStage();
        if (execStage.equalsIgnoreCase("2")) {
            return true;
        } else {
            return false;
        }
    }

    private void fillAndUpdateSparkBlackListToEnvMap(TaskEvent taskEvent, List<String> hostList, long yarnClusterId, MetadataComponentData sparkEssMasterComponentData) {
        long sparkEssMasterComponentId = sparkEssMasterComponentData.getId();
        ComponentNodeDetail sparkEssMasterRunningNode = null;
        List<ComponentNodeDetail> sparkEssMasterNodeDetailList = globalService.getBmrResourceService().queryComponentNodeList(yarnClusterId, sparkEssMasterComponentId);
        // 当前仅有一台spark-ess-master
        if (CollectionUtils.isEmpty(sparkEssMasterNodeDetailList)) {
            logPersist(taskEvent, "sparkEssMaster host list is blank");
        } else {
            List<ComponentNodeDetail> componentNodeRunningStateHosts = sparkEssMasterNodeDetailList.stream()
                    .filter(x -> Constants.APPLICATION_STATE_RUNNING.equals(x.getApplicationState()))
                    .collect(Collectors.toList());
            if (CollectionUtils.isEmpty(componentNodeRunningStateHosts)) {
                logPersist(taskEvent, "sparkEssMaster running node is blank");
            } else {
                sparkEssMasterRunningNode = componentNodeRunningStateHosts.get(0);
            }
        }
        // 更新sparkEssMaster黑名单，把新增的主机列表从黑名单中移除
        List<String> offHostSuffixList = new ArrayList<>();
        hostList.forEach(host -> {
            if (!host.contains(Constants.HOST_SUFFIX)) {
                offHostSuffixList.add(host.concat(Constants.HOST_SUFFIX));
            } else {
                offHostSuffixList.add(host);
            }
        });
        Boolean updateConfigBlackListSuccess = globalService.getBmrConfigService().updateFileIpList(
                sparkEssMasterComponentId, Constants.SPARK_BLACK_LIST, FileOperateType.REMOVE, offHostSuffixList);
        if (updateConfigBlackListSuccess) {
            logPersist(taskEvent, "sparkEssMaster移除黑名单配置成功.");
        }
        // refresh spark ess master black list
        if (!Objects.isNull(sparkEssMasterRunningNode)) {
            boolean sparkEssMasterRefreshStatus = globalService.getSparkEssMasterService().removeBlackList(offHostSuffixList, sparkEssMasterRunningNode.getHostName());
            if (sparkEssMasterRefreshStatus) {
                logPersist(taskEvent, "sparkEssMaster刷新黑名单列表成功.");
            }
        } else {
            logPersist(taskEvent, "sparkEssMaster运行中节点不存在");
        }
    }

    /**
     * 添加节点到节点组,如果节点组不存在则会创建节点组
     *
     * @param taskEvent
     * @param hostList
     * @param yarnClusterId
     */
    private void addOrCreateHostToHostGroup(TaskEvent taskEvent, List<String> hostList, long yarnClusterId, String hostGroupName) {
        List<String> notExistsHostList = globalService.getBmrResourceService().addHostToTideNodeGroup(yarnClusterId, hostList, hostGroupName);
        log.info("addHostToTideNodeGroup find not exist host list is: {}", notExistsHostList);

        if (!CollectionUtils.isEmpty(notExistsHostList)) {
            logPersist(taskEvent, "prepare op addHostToTideNodeGroup find not exist host list is: " + notExistsHostList);
            hostList.removeAll(notExistsHostList);
            if (CollectionUtils.isEmpty(hostList)) {
                logPersist(taskEvent, "available host list is empty, will exit pipeline.");
                throw new IllegalArgumentException(" tide available host list is empty");
            }
        }
    }

    private void fillAndUpdateIncludeFileIntoEnvMap(TaskEvent taskEvent, List<String> hostList, Map<String, Object> instanceEnv, long yarnClusterId) {
        RMInfoObj rmInfo = globalService.getBmrResourceService().queryRMComponentIdByClusterId(yarnClusterId);

        StringJoiner rmHostInfoJoiner = new StringJoiner(Constants.COMMA);
        List<String> rmHostList = rmInfo.getRmHostList();
        rmHostList.forEach(rmHostInfoJoiner::add);
        String rmHostValue = rmHostInfoJoiner.toString();
        instanceEnv.put(Constants.SUB_SYSTEM_HOST_LIST, rmHostValue);

        String msg = String.format(" presto潮汐发布新增额外环境变量: key=%s, value=%s",
                Constants.SUB_SYSTEM_HOST_LIST, rmHostValue);
        logPersist(taskEvent, msg);

        List<ResourceHostInfo> resourceHostInfoList = globalService.getBmrResourceService().queryHostListByName(hostList);
        List<String> ipList = new ArrayList<>();
        resourceHostInfoList.stream().forEach(node -> ipList.add(node.getIp()));
        globalService.getBmrConfigService().updateFileIpList(rmInfo.getComponentId(),
                Constants.YARN_INCLUDE, FileOperateType.ADD, ipList);
        logPersist(taskEvent, "更新yarn-include成功");

        ThreadUtil.sleep(Constants.ONE_SECOND * 2);

        FileDownloadData fileDownloadData = globalService.getBmrConfigService().queryDownloadInfoByComponentId(
                rmInfo.getComponentId(), Constants.YARN_INCLUDE);
        instanceEnv.put(Constants.YARN_INCLUDE_DOWNLOAD_URL, fileDownloadData.getDownloadUrl());
        instanceEnv.put(Constants.YARN_INCLUDE_FILE_MD5, fileDownloadData.getFileMd5());
        instanceEnv.put(Constants.FLOW_ENV_KEY, SpringApplicationContext.getEnv());
        instanceEnv.put(Constants.YARN_RM_COMPONENT_ID_KEY, rmInfo.getComponentId());

        String yarnIncludeMsg = String.format("新增ResourceManager白名单下载链接: %s\n md5=%s",
                fileDownloadData.getDownloadUrl(), fileDownloadData.getFileMd5());
        logPersist(taskEvent, yarnIncludeMsg);
    }

    /**
     * 该函数用于填充安装和配置包相关的环境变量到实例环境中。
     * * 主要功能包括：
     * * 1. 获取Yarn集群的元数据信息，并将其填充到环境变量中。
     * * 2. 设置NodeManager、SparkEssWorker和Amiya组件的下载目录及相关环境变量。
     * * 3. 获取并设置默认的配置包和版本信息。
     * * 4. 设置节点组信息，并为每个主机分配对应的分组名称和硬件信息。
     * * 5. 将最终的环境变量映射转换为JSON字符串并存储。
     *
     * @param taskEvent
     * @param hostList
     * @param instanceEnv
     * @param flowEntity
     * @param yarnClusterId
     * @param nodeManagerComponentData
     * @param sparkEssWorkerComponentData
     * @param amiyaComponentData
     */
    private void fillTideComponentAndPackageInfoToEnvMap(TaskEvent taskEvent, List<String> hostList, Map<String, Object> instanceEnv, ExecutionFlowEntity flowEntity, long yarnClusterId, MetadataComponentData nodeManagerComponentData, MetadataComponentData sparkEssWorkerComponentData, MetadataComponentData amiyaComponentData) {
        final MetadataClusterData metadataClusterData = globalService.getBmrMetadataService().queryClusterDetail(yarnClusterId);
        Preconditions.checkNotNull(metadataClusterData, "yarn cluster info is null");
        instanceEnv.put(Constants.COMPONENT_ROLE, metadataClusterData.getUpperService());
        instanceEnv.put(Constants.COMPONENT_CLUSTER, metadataClusterData.getClusterName());
        instanceEnv.put(Constants.FLOW_ID, flowEntity.getId());
        instanceEnv.put(Constants._JOB_EXCUTE_TYPE, DolpTaskType.JOB_AGENT.name());
        log.info("JOB_EXECUTE_TYPE :" + flowEntity.getJobExecuteType());
        instanceEnv.put(Constants.RELASE_SCOPE, FlowReleaseScopeType.GRAY_RELEASE.name());
        instanceEnv.put(Constants.BATCH_ID, taskEvent.getBatchId());
        String downloadDir = Constants.DOWNLOAD_DIR_VALUE + nodeManagerComponentData.getComponentName() + File.separator
                + flowEntity.getComponentId();
        instanceEnv.put(Constants.DOWNLOAD_DIR, downloadDir);
        log.info("Presto Tide Online : NodeManager download dir is ====== {}", downloadDir);

        // SparkEssWorker 系统变量
        instanceEnv.put(Constants.SPARK_COMPONENT_NAME, "SparkEssWorker");
        String sparkEssWorkerDownloadDir = Constants.DOWNLOAD_DIR_VALUE + "SparkEssWorker" + File.separator
                + sparkEssWorkerComponentData.getId();
        instanceEnv.put(Constants.SPARK_DOWNLOAD_DIR, sparkEssWorkerDownloadDir);
        log.info("Presto Tide Online : SparkEssWorker download dir is ====== {}", sparkEssWorkerDownloadDir);

        // Amiya 系统变量
        instanceEnv.put(Constants.AMIYA_COMPONENT_NAME, "Amiya");
        String amiyaDownloadDir = Constants.DOWNLOAD_DIR_VALUE + "Amiya" + File.separator
                + amiyaComponentData.getId();
        instanceEnv.put(Constants.AMIYA_DOWNLOAD_DIR, amiyaDownloadDir);
        log.info("Presto Tide Online : Amiya download dir is ====== {}", amiyaDownloadDir);

        // NM数量,防止大批量NM节点错误配置失联
        instanceEnv.put(Constants.NODE_WARNINGS_NUMBER, nodeManagerComponentData.getNodeWarningsNumber());

        StringJoiner joiner = new StringJoiner(Constants.COMMA);
        hostList.forEach(joiner::add);
        // 机器列表
        instanceEnv.put(Constants.SYSTEM_JOBAGENT_EXEC_HOSTS, joiner.toString());
        // 服务是否重启
        instanceEnv.put(Constants.SERVICE_RESTART, flowEntity.getRestart());
        // 生效方式
        instanceEnv.put(Constants.EFFECTIVE_MODE, flowEntity.getEffectiveMode());

        // 获取默认配置包和版本信息
        final long nodeManagerComponentId = nodeManagerComponentData.getId();
        long nodeManagerDefaultPackId = globalService.getBmrMetadataService().queryDefaultPackageIdByComponentId(nodeManagerComponentId);
        final MetadataPackageData nodeManagerDefaultPackageData = globalService.getBmrMetadataService().queryPackageDetailById(nodeManagerDefaultPackId);
        Preconditions.checkNotNull(nodeManagerDefaultPackageData, "nodeManager default pack not exist");
        instanceEnv.put(Constants.CI_PACK_ID, nodeManagerDefaultPackageData.getId());
        instanceEnv.put(Constants.CI_PACK_MD5, nodeManagerDefaultPackageData.getProductBagMd5());
        instanceEnv.put(Constants.CI_PACK_NAME, nodeManagerDefaultPackageData.getProductBagName());
        instanceEnv.put(Constants.CI_PACK_TAG_NAME, nodeManagerDefaultPackageData.getTagName());
        String packageDownloadUrl = globalService.getBmrMetadataService().queryPackageDownloadInfo(nodeManagerDefaultPackId);
        instanceEnv.put(Constants.CI_PACK_URL, packageDownloadUrl);

        final long nodeManagerDefaultConfId = globalService.getBmrConfigService().queryDefaultConfigVersionIdByComponentId(nodeManagerComponentId);
        final ConfigDetailData nodeManagerConfData = globalService.getBmrConfigService().queryConfigDetailById(nodeManagerDefaultConfId);
        instanceEnv.put(Constants.CONFIG_PACK_ID, nodeManagerConfData.getId());
        instanceEnv.put(Constants.CONFIG_PACK_MD5, nodeManagerConfData.getConfigVersionMd5());
        instanceEnv.put(Constants.CONFIG_PACK_NAME, nodeManagerConfData.getConfigVersionNumber() + ".zip");
        instanceEnv.put(Constants.CONFIG_PACK_VERSION, nodeManagerConfData.getConfigVersionNumber());
        String configDownloadUrl = nodeManagerConfData.getDownloadUrl();
        instanceEnv.put(Constants.CONFIG_PACK_URL, configDownloadUrl);
        instanceEnv.put(Constants.NODEMANAGER_IS_EXECUTE, Constants.TRUE);

        // 设置节点组信息
        List<ConfigGroupDo> configGroupDoList = globalService.getBmrConfigService().queryConfigGroupInfoById(nodeManagerDefaultConfId);
        log.info("query nodeManger configGroupInfo component id is {}", nodeManagerComponentId);
        log.info("query nodeManger configGroupInfo default conf id is {}", nodeManagerDefaultConfId);
        log.info("query nodeManger configGroupInfo resp configGroupDoList is {}", configGroupDoList);

        ConfigGroupDo defaultConfigGroupDo = null;
        Map<Integer, ConfigGroupDo> configGroupMap = new HashMap<>();
        for (ConfigGroupDo configGroupDo : configGroupDoList) {
            configGroupMap.put(configGroupDo.getLogicGroupId(), configGroupDo);
            if (configGroupDo.isDefaultGroup()) {
                defaultConfigGroupDo = configGroupDo;
            }
        }
        Preconditions.checkNotNull(defaultConfigGroupDo, "配置中心默认分组不存在，请检查");
        Map<String, HostAndLogicGroupInfo> hostGroupInfos = globalService.getBmrResourceService().queryNodeGroupInfo(yarnClusterId, hostList);
        Preconditions.checkState(!CollectionUtils.isEmpty(hostGroupInfos), "host with node group is not find");

        Map<String, Map<String, String>> hostEnvMap = new LinkedHashMap<>();
        for (Map.Entry<String, HostAndLogicGroupInfo> entry : hostGroupInfos.entrySet()) {
            String hostname = entry.getKey();
            HostAndLogicGroupInfo hostAndLogicGroupInfo = entry.getValue();
            int group = hostAndLogicGroupInfo.getLogicGroupId().intValue();
            Map<String, String> hostEnv = new HashMap<>();
            if (configGroupMap.containsKey(group)) {
                hostEnv.put(Constants.CONFIG_NODE_GROUP, configGroupMap.get(group).getDirName());
            } else {
                hostEnv.put(Constants.CONFIG_NODE_GROUP, defaultConfigGroupDo.getDirName());
            }
            // 设置主机对应的分组名称
            hostEnv.put(Constants.NODE_GROUP_NAME, hostAndLogicGroupInfo.getLogicGroupName());
            hostEnvMap.put(hostname, hostEnv);

            hostEnv.put(Constants.NUM_SSD, String.valueOf(hostAndLogicGroupInfo.getNumSsd()));
            hostEnv.put(Constants.NUM_SATA, String.valueOf(hostAndLogicGroupInfo.getNumSata()));
            hostEnv.put(Constants.NUM_NVME, String.valueOf(hostAndLogicGroupInfo.getNvmeTotal()));
        }

        final long sparkEssWorkerComponentId = sparkEssWorkerComponentData.getId();
        long sparkEssWorkerPackageId = globalService.getBmrMetadataService().queryDefaultPackageIdByComponentId(sparkEssWorkerComponentId);
        final MetadataPackageData sparkEssWorkerPackInfo = globalService.getBmrMetadataService().queryPackageDetailById(sparkEssWorkerPackageId);
        /**
         * SparkEssManager相关环境变量
         */
        if (NumberUtils.isPositiveLong(sparkEssWorkerPackageId)) {
            instanceEnv.put(Constants.SPARK_CI_PACK_ID, sparkEssWorkerPackInfo.getId());
            instanceEnv.put(Constants.SPARK_CI_PACK_MD5, sparkEssWorkerPackInfo.getProductBagMd5());
            instanceEnv.put(Constants.SPARK_CI_PACK_NAME, sparkEssWorkerPackInfo.getProductBagName());
            instanceEnv.put(Constants.SPARK_CI_PACK_TAG_NAME, sparkEssWorkerPackInfo.getTagName());
            String sparEssWorkerPackageDownloadUrl = globalService.getBmrMetadataService().queryPackageDownloadInfo(sparkEssWorkerPackageId);
            instanceEnv.put(Constants.SPARK_IS_EXECUTE, Constants.TRUE);
            instanceEnv.put(Constants.SPARK_CI_PACK_URL, sparEssWorkerPackageDownloadUrl);
        } else {
            instanceEnv.put(Constants.SPARK_CI_PACK_ID, Constants.EMPTY_STRING);
            instanceEnv.put(Constants.SPARK_CI_PACK_MD5, Constants.EMPTY_STRING);
            instanceEnv.put(Constants.SPARK_CI_PACK_NAME, Constants.EMPTY_STRING);
            instanceEnv.put(Constants.SPARK_CI_PACK_TAG_NAME, Constants.EMPTY_STRING);
            instanceEnv.put(Constants.SPARK_CI_PACK_VERSION, Constants.EMPTY_STRING);
            instanceEnv.put(Constants.SPARK_CI_PACK_URL, Constants.EMPTY_STRING);
            instanceEnv.put(Constants.SPARK_IS_EXECUTE, Constants.FALSE);
        }

        long sparkEssWorkerDefaultConfId = globalService.getBmrConfigService().queryDefaultConfigVersionIdByComponentId(sparkEssWorkerComponentId);

        if (NumberUtils.isPositiveLong(sparkEssWorkerDefaultConfId)) {
            final ConfigDetailData sparkEssWorkerConfigMetadataEntity = globalService.getBmrConfigService().queryConfigDetailById(sparkEssWorkerDefaultConfId);
            instanceEnv.put(Constants.SPARK_CONFIG_PACK_ID, sparkEssWorkerConfigMetadataEntity.getId());
            instanceEnv.put(Constants.SPARK_CONFIG_PACK_MD5, sparkEssWorkerConfigMetadataEntity.getConfigVersionMd5());
            instanceEnv.put(Constants.SPARK_CONFIG_PACK_NAME, sparkEssWorkerConfigMetadataEntity.getConfigVersionNumber() + ".zip");
            instanceEnv.put(Constants.SPARK_CONFIG_PACK_VERSION, sparkEssWorkerConfigMetadataEntity.getConfigVersionNumber());
            String sparkEssWorkerConfigDownloadUrl = sparkEssWorkerConfigMetadataEntity.getDownloadUrl();
            instanceEnv.put(Constants.SPARK_CONFIG_PACK_URL, sparkEssWorkerConfigDownloadUrl);
            // 设置节点组信息
            List<ConfigGroupDo> sparkEssWorkerConfigGroupDoList = globalService.getBmrConfigService().queryConfigGroupInfoById(sparkEssWorkerDefaultConfId);
            log.info("query spark ess worker configGroupInfo component id is {}", sparkEssWorkerComponentId);
            log.info("query spark ess worker configGroupInfo default conf id is {}", sparkEssWorkerDefaultConfId);
            log.info("query spark ess worker configGroupInfo resp configGroupDoList is {}", sparkEssWorkerConfigGroupDoList);

            ConfigGroupDo sparkEssWorkerDefaultConfigGroupDo = null;
            HashMap<Integer, ConfigGroupDo> sparkEssWorkerConfigGroupMap = new HashMap<>();
            for (ConfigGroupDo sparkEssWorkerConfigGroupDo : sparkEssWorkerConfigGroupDoList) {
                configGroupMap.put(sparkEssWorkerConfigGroupDo.getLogicGroupId(), sparkEssWorkerConfigGroupDo);
                if (sparkEssWorkerConfigGroupDo.isDefaultGroup()) {
                    sparkEssWorkerDefaultConfigGroupDo = sparkEssWorkerConfigGroupDo;
                }
            }
            Preconditions.checkNotNull(sparkEssWorkerDefaultConfigGroupDo, "sparkEssWork配置中心默认分组不存在，请检查");

            for (Map.Entry<String, HostAndLogicGroupInfo> entry : hostGroupInfos.entrySet()) {
                String hostname = entry.getKey();
                HostAndLogicGroupInfo hostAndLogicGroupInfo = entry.getValue();
                int group = hostAndLogicGroupInfo.getLogicGroupId().intValue();
                Map<String, String> hostEnv = hostEnvMap.get(hostname);
                // Map<String, String> hostEnv = hostEnvMap.get(hostname);
                if (configGroupMap.containsKey(group)) {
                    hostEnv.put(Constants.SPARK_CONFIG_NODE_GROUP, sparkEssWorkerConfigGroupMap.get(group).getDirName());
                } else {
                    hostEnv.put(Constants.SPARK_CONFIG_NODE_GROUP, sparkEssWorkerDefaultConfigGroupDo.getDirName());
                }
                hostEnvMap.put(hostname, hostEnv);
            }
        } else {
            instanceEnv.put(Constants.SPARK_CONFIG_PACK_ID, Constants.EMPTY_STRING);
            instanceEnv.put(Constants.SPARK_CONFIG_PACK_MD5, Constants.EMPTY_STRING);
            instanceEnv.put(Constants.SPARK_CONFIG_PACK_NAME, Constants.EMPTY_STRING);
            instanceEnv.put(Constants.SPARK_CONFIG_PACK_VERSION, Constants.EMPTY_STRING);
            instanceEnv.put(Constants.SPARK_CONFIG_PACK_URL, Constants.EMPTY_STRING);
        }

        /**
         * AmiyaManager相关环境变量
         */
        long amiyaComponentId = amiyaComponentData.getId();
        long amiyaDefaultPackageId = globalService.getBmrMetadataService().queryDefaultPackageIdByComponentId(amiyaComponentId);
        if (NumberUtils.isPositiveLong(amiyaDefaultPackageId)) {
            final MetadataPackageData amiyaDefaultPackInfo = globalService.getBmrMetadataService().queryPackageDetailById(amiyaDefaultPackageId);
            instanceEnv.put(Constants.AMIYA_CI_PACK_ID, amiyaDefaultPackInfo.getId());
            instanceEnv.put(Constants.AMIYA_CI_PACK_MD5, amiyaDefaultPackInfo.getProductBagMd5());
            instanceEnv.put(Constants.AMIYA_CI_PACK_NAME, amiyaDefaultPackInfo.getProductBagName());
            instanceEnv.put(Constants.AMIYA_CI_PACK_TAG_NAME, amiyaDefaultPackInfo.getTagName());
            instanceEnv.put(Constants.AMIYA_IS_EXECUTE, Constants.TRUE);
            String amiyaDefaultPackageDownloadUrl = globalService.getBmrMetadataService().queryPackageDownloadInfo(amiyaDefaultPackageId);
            instanceEnv.put(Constants.AMIYA_CI_PACK_URL, amiyaDefaultPackageDownloadUrl);
        } else {
            instanceEnv.put(Constants.AMIYA_CI_PACK_ID, Constants.EMPTY_STRING);
            instanceEnv.put(Constants.AMIYA_CI_PACK_MD5, Constants.EMPTY_STRING);
            instanceEnv.put(Constants.AMIYA_CI_PACK_NAME, Constants.EMPTY_STRING);
            instanceEnv.put(Constants.AMIYA_CI_PACK_TAG_NAME, Constants.EMPTY_STRING);
            instanceEnv.put(Constants.AMIYA_CI_PACK_VERSION, Constants.EMPTY_STRING);
            instanceEnv.put(Constants.AMIYA_CI_PACK_URL, Constants.EMPTY_STRING);
            instanceEnv.put(Constants.AMIYA_IS_EXECUTE, Constants.FALSE);
        }
        long amiyaDefaultConfigId = globalService.getBmrConfigService().queryDefaultConfigVersionIdByComponentId(amiyaComponentId);
        if (NumberUtils.isPositiveLong(amiyaDefaultConfigId)) {
            final ConfigDetailData amiyaDefaultConfigInfo = globalService.getBmrConfigService().queryConfigDetailById(amiyaDefaultConfigId);
            instanceEnv.put(Constants.AMIYA_CONFIG_PACK_ID, amiyaDefaultConfigInfo.getId());
            instanceEnv.put(Constants.AMIYA_CONFIG_PACK_MD5, amiyaDefaultConfigInfo.getConfigVersionMd5());
            instanceEnv.put(Constants.AMIYA_CONFIG_PACK_NAME, amiyaDefaultConfigInfo.getConfigVersionNumber() + ".zip");
            instanceEnv.put(Constants.AMIYA_CONFIG_PACK_VERSION, amiyaDefaultConfigInfo.getConfigVersionNumber());
            String amiyaDefaultConfigDownloadUrl = amiyaDefaultConfigInfo.getDownloadUrl();
            instanceEnv.put(Constants.AMIYA_CONFIG_PACK_URL, amiyaDefaultConfigDownloadUrl);
            // 设置节点组信息

            List<ConfigGroupDo> amiyaDefaultConfigGroupDoList = globalService.getBmrConfigService().queryConfigGroupInfoById(amiyaDefaultConfigId);
            log.info("query amiya configGroupInfo component id is {}", amiyaComponentId);
            log.info("query amiya configGroupInfo default conf id is {}", amiyaDefaultConfigId);
            log.info("query amiya configGroupInfo resp configGroupDoList is {}", amiyaDefaultConfigGroupDoList);
            ConfigGroupDo amiyaDefaultConfigGroupDo = null;
            HashMap<Integer, ConfigGroupDo> amiyaConfigGroupMap = new HashMap<>();
            for (ConfigGroupDo configGroupDo : amiyaDefaultConfigGroupDoList) {
                amiyaConfigGroupMap.put(configGroupDo.getLogicGroupId(), configGroupDo);
                if (configGroupDo.isDefaultGroup()) {
                    amiyaDefaultConfigGroupDo = configGroupDo;
                }
            }

            Preconditions.checkNotNull(amiyaDefaultConfigGroupDo, "配置中心默认分组不存在，请检查");

            for (Map.Entry<String, HostAndLogicGroupInfo> entry : hostGroupInfos.entrySet()) {
                String hostname = entry.getKey();
                HostAndLogicGroupInfo hostAndLogicGroupInfo = entry.getValue();
                int group = hostAndLogicGroupInfo.getLogicGroupId().intValue();
                Map<String, String> hostEnv = hostEnvMap.get(hostname);
                //Map<String, String> hostEnv = hostEnvMap.get(hostname);
                if (amiyaConfigGroupMap.containsKey(group)) {
                    hostEnv.put(Constants.AMIYA_CONFIG_NODE_GROUP, amiyaConfigGroupMap.get(group).getDirName());
                } else {
                    hostEnv.put(Constants.AMIYA_CONFIG_NODE_GROUP, amiyaDefaultConfigGroupDo.getDirName());
                }
                hostEnvMap.put(hostname, hostEnv);

                hostEnv.put(Constants.NUM_SSD, String.valueOf(hostAndLogicGroupInfo.getNumSsd()));
                hostEnv.put(Constants.NUM_SATA, String.valueOf(hostAndLogicGroupInfo.getNumSata()));
                hostEnv.put(Constants.NUM_NVME, String.valueOf(hostAndLogicGroupInfo.getNvmeTotal()));
            }
        } else {
            instanceEnv.put(Constants.AMIYA_CONFIG_PACK_ID, Constants.EMPTY_STRING);
            instanceEnv.put(Constants.AMIYA_CONFIG_PACK_MD5, Constants.EMPTY_STRING);
            instanceEnv.put(Constants.AMIYA_CONFIG_PACK_NAME, Constants.EMPTY_STRING);
            instanceEnv.put(Constants.AMIYA_CONFIG_PACK_VERSION, Constants.EMPTY_STRING);
            instanceEnv.put(Constants.AMIYA_CONFIG_PACK_URL, Constants.EMPTY_STRING);
        }
        instanceEnv.put(Constants.HOST_ENV_MAP_KEY, JSONUtil.toJsonStr(hostEnvMap));
    }

}
