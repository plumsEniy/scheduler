package com.bilibili.cluster.scheduler.api.service.presto;

import cn.hutool.core.net.url.UrlBuilder;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.json.JSONUtil;
import com.bilibili.cluster.scheduler.api.service.bmr.config.BmrConfigService;
import com.bilibili.cluster.scheduler.api.service.bmr.metadata.BmrMetadataService;
import com.bilibili.cluster.scheduler.api.service.bmr.resourceV2.BmrResourceV2Service;
import com.bilibili.cluster.scheduler.api.service.caster.CasterService;
import com.bilibili.cluster.scheduler.api.service.caster.ComCasterService;
import com.bilibili.cluster.scheduler.api.tools.FreemarkUtils;
import com.bilibili.cluster.scheduler.common.Constants;
import com.bilibili.cluster.scheduler.common.dto.bmr.config.ComponentConfigVersionEntity;
import com.bilibili.cluster.scheduler.common.dto.bmr.config.ConfigData;
import com.bilibili.cluster.scheduler.common.dto.bmr.config.ConfigDetailData;
import com.bilibili.cluster.scheduler.common.dto.bmr.config.model.ConfigFileEntity;
import com.bilibili.cluster.scheduler.common.dto.bmr.config.model.ConfigGroupRelationEntity;
import com.bilibili.cluster.scheduler.common.dto.bmr.config.model.ConfigItem;
import com.bilibili.cluster.scheduler.common.dto.bmr.config.model.ConfigItemInfo;
import com.bilibili.cluster.scheduler.common.dto.bmr.metadata.MetadataClusterData;
import com.bilibili.cluster.scheduler.common.dto.bmr.metadata.enums.ClusterNetworkEnvironmentEnum;
import com.bilibili.cluster.scheduler.common.dto.bmr.resourceV2.TideNodeDetail;
import com.bilibili.cluster.scheduler.common.dto.presto.*;
import com.bilibili.cluster.scheduler.common.dto.presto.template.PrestoCasterConfig;
import com.bilibili.cluster.scheduler.common.dto.presto.template.PrestoCasterTemplate;
import com.bilibili.cluster.scheduler.common.dto.presto.template.PrestoDeployDTO;
import com.bilibili.cluster.scheduler.common.enums.resourceV2.TideClusterType;
import com.bilibili.cluster.scheduler.common.enums.resourceV2.TideNodeStatus;
import com.bilibili.cluster.scheduler.common.enums.bmr.config.ConfigFileTypeEnum;
import com.bilibili.cluster.scheduler.common.enums.bmr.config.ConfigVersionType;
import com.bilibili.cluster.scheduler.common.enums.template.TemplateEnum;
import com.bilibili.cluster.scheduler.common.response.BaseMsgResp;
import com.bilibili.cluster.scheduler.common.response.ResponseResult;
import com.bilibili.cluster.scheduler.common.utils.BaseRespUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.io.ByteArrayInputStream;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @description: presto
 * @Date: 2024/6/7 14:34
 * @Author: nizhiqiang
 */

@Slf4j
@Service
public class PrestoServiceImpl implements PrestoService {

    private Pattern memoryPattern = Pattern.compile("[\\d\\.]+G");

    @Resource
    BmrMetadataService bmrMetadataService;

    @Resource
    BmrConfigService bmrConfigService;

    @Resource
    ComCasterService comCasterService;

    @Resource
    CasterService casterService;

    @Value("${spring.profiles.active}")
    private String active;

    @Resource
    BmrResourceV2Service bmrResourceV2Service;

    @Value("${com.caster.trino.cluster.id:126}")
    long trinoClusterId;

    @Override
    public String queryPrestoTemplate(long clusterId, long configVersionId, String image) {
        PrestoYamlObj prestoYamlObj = buildPrestoYamlObj(configVersionId, image);
        return FreemarkUtils.fillTemplate(TemplateEnum.PRESTO, prestoYamlObj);
    }

    @Override
    public void deployPresto(long clusterId, long configVersionId, String image) {
        PrestoDeployDTO prestoDeployDTO = generateDeployPrestoReq(clusterId, configVersionId, image);

        log.info("presto deploy is {}", JSONUtil.toJsonStr(prestoDeployDTO));

        casterService.deployPresto(prestoDeployDTO);
    }

    @Override
    public String getDeployPrestoTemplate(long clusterId, long configVersionId, String image) {

        PrestoDeployDTO prestoDeployDTO = generateDeployPrestoReq(clusterId, configVersionId, image);
        log.info(" get presto deploy template is {}", JSONUtil.toJsonStr(prestoDeployDTO));
        prestoDeployDTO.setPreview(true);
        return casterService.deployPresto(prestoDeployDTO);

    }

    @Override
    public PrestoDeployDTO generateDeployPrestoReq(long clusterId, long configVersionId, String image) {
        PrestoYamlObj prestoYamlObj = buildPrestoYamlObj(configVersionId, image);

        MetadataClusterData clusterData = bmrMetadataService.queryClusterDetail(clusterId);

        PrestoDeployDTO prestoDeployDTO = new PrestoDeployDTO();
        prestoDeployDTO.setAppId(clusterData.getAppId());
        prestoDeployDTO.setEnv(clusterData.getNetworkEnvironment().getEnv());
        prestoDeployDTO.setClusterName(Constants.PRESTO_CASTER_CLUSTER_NAME);
        prestoDeployDTO.setResourcePoolName("trino");
        prestoDeployDTO.setPreview(false);

        String template = FreemarkUtils.fillTemplate(TemplateEnum.PRESTO, prestoYamlObj);
        PrestoCasterTemplate prestoCasterTemplate = new PrestoCasterTemplate();
        prestoCasterTemplate.setSpec(template);
        prestoCasterTemplate.setCoordinator(convertToCasterConfig(prestoYamlObj.getCoordinator()));
        prestoCasterTemplate.setResource(convertToCasterConfig(prestoYamlObj.getResource()));
        prestoCasterTemplate.setWorker(convertToCasterConfig(prestoYamlObj.getWorker()));
        prestoCasterTemplate.setTrinoName(clusterData.getClusterName());
        prestoCasterTemplate.setImage(image);
        prestoDeployDTO.setTemplate(prestoCasterTemplate);
        return prestoDeployDTO;
    }

    @Override
    public PrestoNodeInfo queryNodesByTemplate(long clusterId, long configVersionId) {
        PrestoYamlObj prestoYamlObj = buildPrestoYamlObj(configVersionId, Constants.EMPTY_STRING);
        PrestoNodeInfo prestoNodeInfo = new PrestoNodeInfo();
        prestoNodeInfo.setCoordinator(prestoYamlObj.getCoordinator());
        prestoNodeInfo.setWorker(prestoYamlObj.getWorker());
        prestoNodeInfo.setResource(prestoYamlObj.getResource());
        BeanUtils.copyProperties(prestoNodeInfo, prestoYamlObj);
        return prestoNodeInfo;
    }

    @Override
    public void activeCluster(String clusterName, String env) {
        String url = UrlBuilder.ofHttp(getPrestoUrl(env))
                .addPath("/gateway/api/active")
                .addPath(clusterName)
                .build();

        log.info("active presto cluster url is {}", url);
        String respStr = HttpRequest.post(url)
                .execute()
                .body();
        log.info("active presto cluster resp is {}", respStr);

        BaseMsgResp baseMsgResp = JSONUtil.toBean(respStr, BaseMsgResp.class);
        BaseRespUtil.checkMsgResp(baseMsgResp);
    }

    @Override
    public void deactivateCluster(String clusterName, String env) {
        String url = UrlBuilder.ofHttp(getPrestoUrl(env))
                .addPath("/gateway/api/deactivate")
                .addPath(clusterName)
                .build();

        log.info("deactivate presto cluster url is {}", url);
        String respStr = HttpRequest.post(url)
                .execute()
                .body();
        log.info("deactivate presto cluster resp is {}", respStr);

        BaseMsgResp baseMsgResp = JSONUtil.toBean(respStr, BaseMsgResp.class);
        BaseRespUtil.checkMsgResp(baseMsgResp);
    }


    /**
     * 将presto配置转换成caster中的配置
     *
     * @param prestoConfig
     * @return
     */
    private PrestoCasterConfig convertToCasterConfig(PrestoConfig prestoConfig) {
        if (prestoConfig == null) {
            return null;
        }

        PrestoCasterConfig casterConfig = new PrestoCasterConfig();
        casterConfig.setCount(prestoConfig.getCount());
        casterConfig.setCpu(Integer.valueOf(prestoConfig.getCpuLimit()) * 1000);
        String memoryLimit = prestoConfig.getMemoryLimit();
        Matcher memoryMatcher = memoryPattern.matcher(memoryLimit);
        if (!memoryMatcher.find()) {
            throw new IllegalArgumentException("memory is not format, memory str is " + memoryLimit);
        }
        Integer memory = Integer.valueOf(memoryLimit.substring(0, memoryLimit.length() - 1)) * 1024;
        casterConfig.setMem(memory);
        return casterConfig;
    }

    /**
     * 构建presto
     *
     * @param configVersionId
     * @param image
     * @return
     */
    @Override
    public PrestoYamlObj buildPrestoYamlObj(long configVersionId, String image) {
        PrestoYamlObj prestoYamlObj = new PrestoYamlObj();
        prestoYamlObj.setImageName(image);

        ConfigGroupRelationEntity defaultGroupRelation = bmrConfigService.queryDefaultGroupRelation(configVersionId);
        if (defaultGroupRelation == null) {
            throw new IllegalArgumentException("default group is not exist");
        }
        Long defaultGroupId = defaultGroupRelation.getId();

        List<ConfigFileEntity> configFileEntityList = bmrConfigService.queryFileListByGroupId(defaultGroupId);
        List<PrestoCatalog> catalogList = new ArrayList<>();
        prestoYamlObj.setCatalogList(catalogList);

        for (ConfigFileEntity configFileEntity : configFileEntityList) {
            String fileName = configFileEntity.getFileName();
            if (!(fileName.startsWith(Constants.START_CATALOG))) {
                continue;
            }

            String filePrefix = null;
            List<ConfigItem> configItemList = new ArrayList<>();

            if (fileName.endsWith(Constants.XML_SUFFIX)) {
                ConfigData cataLogData = bmrConfigService.queryItemListAndData(configVersionId, null
                        , fileName, ConfigFileTypeEnum.MAP, ConfigVersionType.NORMAL);
                List<ConfigItemInfo> cataLogItemInfoList = cataLogData.getItems();
                if (CollectionUtils.isEmpty(cataLogItemInfoList)) {
                    continue;
                }
                filePrefix = StrUtil.removeSuffix(fileName, Constants.XML_SUFFIX);
                configItemList = ConfigItem.fillConfigItemInfoIntoConfigItem(cataLogItemInfoList);
            } else if (fileName.endsWith(Constants.PROPERTIES_SUFFIX)) {
                ConfigData cataLogData = bmrConfigService.queryItemListAndData(configVersionId, null
                        , fileName, ConfigFileTypeEnum.STRING, ConfigVersionType.NORMAL);
                String propertiesStr = cataLogData.getContext();
                Properties properties = convertPropertyStrToProperty(propertiesStr);
                for (Map.Entry<Object, Object> entry : properties.entrySet()) {
                    String key = entry.getKey().toString();
                    String value = entry.getValue().toString();
                    ConfigItem configItem = new ConfigItem(key, value);
                    configItemList.add(configItem);
                }
                filePrefix = StrUtil.removeSuffix(fileName, Constants.PROPERTIES_SUFFIX);

            } else {
                log.info("not handler presto cata log file, file name is " + fileName);
                continue;
            }

            PrestoCatalog catalog = new PrestoCatalog();
            filePrefix = StrUtil.removePrefix(filePrefix, Constants.START_CATALOG);
            catalog.setFileName(filePrefix);
            catalog.setConfigItemList(configItemList);
            catalogList.add(catalog);
        }

        List<String> fileNameList = configFileEntityList.stream().map(ConfigFileEntity::getFileName).collect(Collectors.toList());

        ConfigData clusterData = bmrConfigService.queryItemListAndData(configVersionId, null
                , Constants.CLUSTER_FILE, ConfigFileTypeEnum.MAP, ConfigVersionType.NORMAL);
        List<ConfigItemInfo> clusterItemInfoList = clusterData.getItems();
        Map<String, String> clusterItemMap = clusterItemInfoList.stream()
                .collect(Collectors.toMap(ConfigItemInfo::getItemKey, ConfigItemInfo::getItemValue));
        String capacity = clusterItemMap.get("capacity");
        prestoYamlObj.setCapacity(capacity);

        String start = clusterItemMap.get("start");
        prestoYamlObj.setStart(start);

        List<String> configFileNameList = fileNameList;

        ConfigDetailData configDetailData = bmrConfigService.queryConfigDetailById(configVersionId);
        Long componentId = configDetailData.getComponentId();
        PrestoConfig coordinatorConfig = fillPrestoConfigIntoPrestoYaml(componentId, configVersionId, configFileNameList, Constants.COORDINATOR, ConfigVersionType.NORMAL);
        prestoYamlObj.setCoordinator(coordinatorConfig);

        if (fileNameList.contains(Constants.RESOURCE_FILE)) {
            PrestoConfig resourceConfig = fillPrestoConfigIntoPrestoYaml(componentId, configVersionId, configFileNameList, Constants.RESOURCE, ConfigVersionType.NORMAL);
            prestoYamlObj.setResource(resourceConfig);
        }

        PrestoConfig workerConfig = fillPrestoConfigIntoPrestoYaml(componentId, configVersionId, configFileNameList, Constants.WORKER, ConfigVersionType.SPECIAL);
        prestoYamlObj.setWorker(workerConfig);

        List<PrestoAdditionalProps> prestoAdditionalPropsList = new ArrayList<>();
        for (ConfigFileEntity configFileEntity : configFileEntityList) {
            String fileName = configFileEntity.getFileName();
            if (!isPrestoAdditionalPropsFile(fileName)) {
                continue;
            }

            ConfigData prestoAdditionalItemData = bmrConfigService.queryItemListAndData(configVersionId, null
                    , fileName, ConfigFileTypeEnum.STRING, ConfigVersionType.NORMAL);
            PrestoAdditionalProps prestoAdditionalProps = new PrestoAdditionalProps();
            prestoAdditionalProps.setKey(fileName);
            prestoAdditionalProps.setValueList(Arrays.asList(prestoAdditionalItemData.getContext().split("\n")));
            prestoAdditionalPropsList.add(prestoAdditionalProps);
        }
        prestoYamlObj.setAdditionalPrestoPropsList(prestoAdditionalPropsList);

        return prestoYamlObj;
    }

    private boolean isPrestoAdditionalPropsFile(String fileName) {
        if (fileName.startsWith(Constants.START_CATALOG)) {
            return false;
        }
        if (fileName.equals(Constants.WORKER_FILE) || fileName.equals(Constants.COORDINATOR_FILE) || fileName.equals(Constants.RESOURCE_FILE)) {
            return false;
        }
        if (fileName.startsWith(Constants.WORKER_ADDITIONALPROPS_FILE_PRE_FIX)
                || fileName.startsWith(Constants.COORDINATOR_ADDITIONALPROPS_FILE_PRE_FIX)
                || fileName.startsWith(Constants.RESOURCE_ADDITIONALPROPS_FILE_PRE_FIX)) {
            return false;
        }

        if (fileName.equals(Constants.CLUSTER_FILE)) {
            return false;
        }

        return true;
    }

    @Override
    public String queryPodLog(long clusterId, String podName, int limit) {
        MetadataClusterData clusterData = bmrMetadataService.queryClusterDetail(clusterId);
        String appId = clusterData.getAppId();
        ClusterNetworkEnvironmentEnum networkEnvironment = clusterData.getNetworkEnvironment();
        String container = podName.replace("replicaset", "container");
        String[] containerPartArray = container.split(Constants.LINE, 2);
        if (containerPartArray.length == 2) {
            // 截取"-"后面的部分的前8位
            String suffix = containerPartArray[1].substring(0, Math.min(8, containerPartArray[1].length()));
            // 拼接回新的字符串
            container = containerPartArray[0] + Constants.LINE + suffix;
        } else {
            throw new RuntimeException("can not find pod log, pod name is illegal,pod name is " + podName);
        }
        String podLog = casterService.queryLog(podName, appId, networkEnvironment.getEnv(), Constants.PRESTO_CASTER_CLUSTER_NAME, limit, container);
        log.info("log is {}", podLog);
        return podLog;
    }

    @Override
    public PrestoNodeInfo queryNodesByComponentId(long clusterId, long componentId) {
        List<ComponentConfigVersionEntity> componentConfigVersionList = bmrConfigService.queryComponentConfigVersionList(componentId, Constants.EMPTY_STRING, 1, 1);
        if (CollectionUtils.isEmpty(componentConfigVersionList)) {
            throw new RuntimeException("presto组件未初始化配置");
        }
        ComponentConfigVersionEntity componentConfigVersionEntity = componentConfigVersionList.get(0);
        return queryNodesByTemplate(clusterId, componentConfigVersionEntity.getId());
    }

    @Override
    public ResponseResult cancelNodeTaintStatus(String hostname) {
        final TideNodeDetail nodeDetail = bmrResourceV2Service.queryTideNodeDetail(hostname);
        if (Objects.isNull(nodeDetail)) {
            return ResponseResult.getError("not found hostname: " + hostname);
        }
        String nodeIp = nodeDetail.getIp();
        boolean isSuc = comCasterService.updateNodeToTaintOff(getPrestoCasterClusterId(), nodeIp, TideClusterType.PRESTO);
        if (!isSuc) {
            return ResponseResult.getError("invoker com caster updateNodeToTaintOff failed");
        }
        isSuc = bmrResourceV2Service.updateTideNodeServiceAndStatus(hostname, TideNodeStatus.AVAILABLE, "", "presto", TideClusterType.PRESTO);
        if (!isSuc) {
            return ResponseResult.getError("invoker bmr update presto node state failed");
        }

        return ResponseResult.getSuccess("ok");
    }

    @Override
    public ResponseResult addNodeTaintStatus(String hostname, String appId) {
        final TideNodeDetail nodeDetail = bmrResourceV2Service.queryTideNodeDetail(hostname);
        if (Objects.isNull(nodeDetail)) {
            return ResponseResult.getError("not found hostname: " + hostname);
        }
        String nodeIp = nodeDetail.getIp();
        boolean isSuc = comCasterService.updateNodeToTaintOn(getPrestoCasterClusterId(), nodeIp, TideClusterType.PRESTO);
        if (!isSuc) {
            return ResponseResult.getError("invoker com caster updateNodeToTaintOn failed");
        }
        isSuc = bmrResourceV2Service.updateTideNodeServiceAndStatus(hostname,
                TideNodeStatus.STAIN, appId, "NodeManager,SparkEssWorker,amiya", TideClusterType.PRESTO);
        if (!isSuc) {
            return ResponseResult.getError("invoker bmr update presto node state failed");
        }

        return ResponseResult.getSuccess("ok");
    }

    @Override
    public long getPrestoCasterClusterId() {
        return trinoClusterId;
    }

    private PrestoConfig fillPrestoConfigIntoPrestoYaml(long componentId, long configVersionId, List<String> configFileNameList, String prestoConfigName, ConfigVersionType configVersionType) {
        String coordinatorAdditionFileName = Constants.EMPTY_STRING;
        String additionalFilePrefix = prestoConfigName + Constants.ADDITIONAL_PROPS;
        if (configFileNameList.contains(additionalFilePrefix + Constants.XML_SUFFIX)) {
            coordinatorAdditionFileName = additionalFilePrefix + Constants.XML_SUFFIX;
        } else if (configFileNameList.contains(additionalFilePrefix + Constants.PROPERTIES_SUFFIX)) {
            coordinatorAdditionFileName = additionalFilePrefix + Constants.PROPERTIES_SUFFIX;
        }
        return buildPrestoConfig(componentId, configVersionId, prestoConfigName + Constants.XML_SUFFIX, coordinatorAdditionFileName, configVersionType);
    }

    private Properties convertPropertyStrToProperty(String propertiesStr) {
        Properties properties = new Properties();
        if (StringUtils.isEmpty(propertiesStr)) {
            return properties;
        }
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(propertiesStr.getBytes())) {
            properties.load(inputStream);

        } catch (Exception e) {
            log.error("transfer property error, property str is " + propertiesStr);
            throw new RuntimeException(e.getMessage());
        }
        return properties;
    }

    @NotNull
    private PrestoConfig buildPrestoConfig(long componentId, long configVersionId, String xmlFile, String extraXmlFile, ConfigVersionType configVersionType) {
        PrestoConfig prestoConfig = new PrestoConfig();
        ConfigData configData = null;
        switch (configVersionType) {
            case NORMAL:
                configData = bmrConfigService.queryItemListAndData(configVersionId, null, xmlFile, ConfigFileTypeEnum.MAP, configVersionType);
                break;
            case SPECIAL:
                configData = bmrConfigService.queryItemListAndData(null, componentId, xmlFile, ConfigFileTypeEnum.MAP, configVersionType);
                break;
        }
        List<ConfigItemInfo> itemInfoList = configData.getItems();

        if (CollectionUtils.isEmpty(itemInfoList)) {
            return null;
        }

        if (!CollectionUtils.isEmpty(itemInfoList)) {
            Map<String, String> coordinatorItemMap = itemInfoList.stream().collect(Collectors.toMap(ConfigItemInfo::getItemKey, ConfigItemInfo::getItemValue));
            prestoConfig.setMemoryLimit(coordinatorItemMap.get("memoryLimit"));
            prestoConfig.setCpuLimit(coordinatorItemMap.get("cpuLimit"));
            prestoConfig.setCount(Integer.valueOf(coordinatorItemMap.get("count")));
            prestoConfig.setLocalDiskEnabled(Boolean.valueOf(coordinatorItemMap.get("localDiskEnabled")));

            String additionalJVMConfig = coordinatorItemMap.get("additionalJVMConfig");
            if (!StringUtils.isEmpty(additionalJVMConfig)) {
                List<String> additionalJVMConfigList = Arrays.asList(additionalJVMConfig.split("\n"));
                prestoConfig.setAdditionalJVMConfigList(additionalJVMConfigList);
            }
        }

//        额外文件走正常逻辑
        if (extraXmlFile.endsWith(Constants.XML_SUFFIX)) {
            ConfigData extraItemData = bmrConfigService.queryItemListAndData(configVersionId, null, extraXmlFile, ConfigFileTypeEnum.MAP, ConfigVersionType.NORMAL);
            List<ConfigItemInfo> extraItemInfoList = extraItemData.getItems();
            if (!CollectionUtils.isEmpty(extraItemInfoList)) {
                Map<String, String> coordinatorExtraItemMap = extraItemInfoList.stream().collect(Collectors.toMap(ConfigItemInfo::getItemKey, ConfigItemInfo::getItemValue));
                prestoConfig.setAdditionalPropMap(coordinatorExtraItemMap);
            }
        } else if (extraXmlFile.endsWith(Constants.PROPERTIES_SUFFIX)) {
            ConfigData cataLogData = bmrConfigService.queryItemListAndData(configVersionId, null, extraXmlFile, ConfigFileTypeEnum.STRING, ConfigVersionType.NORMAL);
            String propertiesStr = cataLogData.getContext();
            Properties properties = convertPropertyStrToProperty(propertiesStr);
            HashMap<String, String> coordinatorExtraItemMap = new HashMap<>();
            for (Map.Entry<Object, Object> entry : properties.entrySet()) {
                String key = entry.getKey().toString();
                String value = entry.getValue().toString();
                coordinatorExtraItemMap.put(key, value);
            }
            prestoConfig.setAdditionalPropMap(coordinatorExtraItemMap);
        }

        return prestoConfig;
    }

    public String getPrestoUrl(String env) {
        if (Constants.UAT_ENV.equalsIgnoreCase(env)) {
            return Constants.UAT_PRESTO_BASE_URL;
        }
        if (Constants.PRE_ENV.equalsIgnoreCase(env)) {
            return Constants.PRE_PRESTO_BASE_URL;
        }
        if (Constants.PROD_ENV.equalsIgnoreCase(env)) {
            return Constants.PROD_PRESTO_BASE_URL;
        }

        throw new RuntimeException("un handler env:" + env);
    }
}
