package com.bilibili.cluster.scheduler.api.service.clickhouse.clickhouse;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.net.url.UrlBuilder;
import com.bilibili.cluster.scheduler.api.service.bmr.config.BmrConfigService;
import com.bilibili.cluster.scheduler.api.service.bmr.metadata.BmrMetadataService;
import com.bilibili.cluster.scheduler.api.service.caster.CasterService;
import com.bilibili.cluster.scheduler.common.Constants;
import com.bilibili.cluster.scheduler.common.dto.bmr.config.ConfigData;
import com.bilibili.cluster.scheduler.common.dto.bmr.config.ConfigDetailData;
import com.bilibili.cluster.scheduler.common.dto.bmr.config.model.ConfigGroupRelationEntity;
import com.bilibili.cluster.scheduler.common.dto.bmr.config.model.ConfigItemInfo;
import com.bilibili.cluster.scheduler.common.dto.bmr.metadata.MetadataClusterData;
import com.bilibili.cluster.scheduler.common.dto.bmr.metadata.MetadataComponentData;
import com.bilibili.cluster.scheduler.common.dto.clickhouse.clickhouse.ClickhouseDeployDTO;
import com.bilibili.cluster.scheduler.common.dto.clickhouse.clickhouse.PodTemplateDTO;
import com.bilibili.cluster.scheduler.common.dto.clickhouse.clickhouse.ShardAllocationDTO;
import com.bilibili.cluster.scheduler.common.dto.clickhouse.clickhouse.templates.*;
import com.bilibili.cluster.scheduler.common.dto.clickhouse.clickhouse.templates.configFile.*;
import com.bilibili.cluster.scheduler.common.dto.clickhouse.clickhouse.templates.template.*;
import com.bilibili.cluster.scheduler.common.enums.bmr.config.ConfigFileTypeEnum;
import com.bilibili.cluster.scheduler.common.enums.bmr.config.ConfigVersionType;
import com.bilibili.cluster.scheduler.common.enums.clickhouse.CKClusterType;
import com.bilibili.cluster.scheduler.common.utils.ObjectTransferUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @description: ck服务
 * @Date: 2025/1/24 14:47
 * @Author: nizhiqiang
 */

@Slf4j
@Service
public class ClickhouseServiceImpl implements ClickhouseService {

    @Resource
    BmrConfigService bmrConfigService;

    @Resource
    BmrMetadataService bmrMetadataService;

    @Resource
    CasterService casterService;

    private Pattern memoryPattern = Pattern.compile("(\\d+)(M|Gi)");
    private Pattern cpuPattern = Pattern.compile("^(\\d+)(m)?$");


    @Override
    public List<PodTemplateDTO> queryPodTemplateList(long configVersionId) {
        List<FilePodTemplate> podTemplateList = buildObjByYamlFile(configVersionId, Constants.POD_TEMPLATES_FILE, new TypeReference<List<FilePodTemplate>>() {
        }, ConfigVersionType.NORMAL, Function.identity());

        List<PodTemplateDTO> podTemplateDTOList = podTemplateList.stream().map(podTemplate -> {
            PodTemplateDTO podTemplateDTO = new PodTemplateDTO();
            podTemplateDTO.setTemplateName(podTemplate.getName());
            podTemplateDTO.setImage(podTemplate.getSpec().getContainer().getImage());
            podTemplateDTO.setRequests(podTemplate.getSpec().getContainer().getResources().getRequests());
            podTemplateDTO.setLimits(podTemplate.getSpec().getContainer().getResources().getLimits());
            return podTemplateDTO;
        }).collect(Collectors.toList());
        return podTemplateDTOList;
    }

    @Override
    public ClickhouseDeployDTO buildScaleDeployDTO(long configVersionId, String podTemplate, List<Integer> shardAllocationList) {
        Assert.isTrue(!CollectionUtils.isEmpty(shardAllocationList), "shard allocation list can not be null");

        ClickhouseDeployDTO clickhouseDeployDTO = buildClickhouseDeployDTO(configVersionId);

        List<ClickhouseCluster> clusterList = clickhouseDeployDTO.getChConfig().getClusters();
        for (ClickhouseCluster clickhouseCluster : clusterList) {

            CKClusterType clusterType = clickhouseCluster.getClusterType();
            List<Shards> newShardList = new LinkedList<>();
            switch (clusterType) {
                case REPLICA:
                    newShardList = scaleShards(configVersionId, podTemplate, shardAllocationList, clickhouseCluster, Constants.REPLICA_PROPS_FILE);
                    clickhouseCluster.getLayout().setShards(newShardList);
                    break;
                case ADMIN:
                    newShardList = scaleShards(configVersionId, podTemplate, splitList(shardAllocationList), clickhouseCluster, Constants.ADMIN_PROPS_FILE);
                    clickhouseCluster.getLayout().setShards(newShardList);
                    break;
            }
        }
        return clickhouseDeployDTO;
    }

    /**
     * 从节点名称中提取节点ID
     *
     * @param nodeName 节点名称，格式为"node-{nodeId}-0"
     * @return 节点ID，如果节点名称格式不正确，则返回null
     */
    private Integer getNodeId(String nodeName) {
        Integer nodeId = null;
        String regex = ".*-([0-9]+)-0$";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(nodeName);
        if (matcher.find()) {
            String nodeIdStr = matcher.group(1);
            nodeId = Integer.valueOf(nodeIdStr);
        } else {
            throw new IllegalArgumentException("can not find node id, node name is  " + nodeName);
        }
        return nodeId;
    }

    /**
     * 迭代只需要根据id进行更新replicas的podTemplate模版即可
     *
     * @param configVersionId 配置版本ID，用于构建Clickhouse部署DTO
     * @param podTemplate     Pod模板字符串，将被应用到符合条件的replicas
     * @param nodeList        节点列表，用于确定哪些replicas需要更新模板
     * @return 返回一个更新后的Clickhouse部署DTO对象
     */
    @Override
    public ClickhouseDeployDTO buildIterationDeployDTO(long configVersionId, String podTemplate, List<String> nodeList) {
        Assert.isTrue(!CollectionUtils.isEmpty(nodeList), "iterate node list can not be null");

        ClickhouseDeployDTO clickhouseDeployDTO = buildClickhouseDeployDTO(configVersionId);
        List<ClickhouseCluster> clusterList = clickhouseDeployDTO.getChConfig().getClusters();
        // 将节点列表转换为节点ID列表
        List<Integer> nodeIdList = nodeList.stream().map(nodeName -> getNodeId(nodeName)).collect(Collectors.toList());

        for (ClickhouseCluster clickhouseCluster : clusterList) {
            List<Shards> shardList = clickhouseCluster.getLayout().getShards();
            for (Shards shard : shardList) {
                for (Replica replica : shard.getReplicas()) {
                    // 获取replica的名称并转换为整数ID
                    String replicaName = replica.getName();
                    Integer replicaId = Integer.valueOf(replicaName);
                    // 如果当前replica的ID在节点ID列表中，则更新其模板
                    if (nodeIdList.contains(replicaId)) {
                        ShardTemplate shardTemplate = replica.getTemplates();
                        shardTemplate.setPodTemplate(podTemplate);
                    }
                }
            }
        }
        // 返回更新后的Clickhouse部署DTO对象
        return clickhouseDeployDTO;
    }

    @Override
    public PaasConfig getPaasConfig(long configVersionId) {
        return buildPaasConfig(configVersionId);
    }

    /**
     * 从replica中获取shard分配，如果shard为空则从admin中获取（admin中默认为shard数量，因为admin中1个shard中只有1个实例）
     *
     * @param configVersionId
     * @return
     */
    @Override
    public ShardAllocationDTO queryShardList(long configVersionId) {
        ShardAllocationDTO shardAllocationDTO = new ShardAllocationDTO();
//        如果replica存在从replica获取
        List<Shards> replicaShardList = buildShards(configVersionId, Constants.REPLICA_SHARDS_FILE);
        List<Integer> replicaAllocationList = buildShardAllocationList(replicaShardList);
        shardAllocationDTO.setReplicaAllocationList(replicaAllocationList);

        List<Shards> adminShardList = buildShards(configVersionId, Constants.ADMIN_SHARDS_FILE);
        List<Integer> adminAllocationList = buildShardAllocationList(adminShardList);
        shardAllocationDTO.setAdminAllocationList(adminAllocationList);

        return shardAllocationDTO;
    }

    private List<Integer> buildShardAllocationList(List<Shards> shardList) {
        Integer lastShardId = 1;
        List<Integer> shardAllocationList = new ArrayList<>();

        if (!CollectionUtils.isEmpty(shardList)) {
            for (Shards shard : shardList) {
                Replica firstReplica = shard.getReplicas().get(0);
                Integer firstReplicaIndex = Integer.valueOf(firstReplica.getName());
                for (int i = lastShardId; i < firstReplicaIndex; i++, lastShardId++) {
                    shardAllocationList.add(0);
                }
                Integer replicasCount = shard.getReplicasCount();
                shardAllocationList.add(replicasCount);
                lastShardId += replicasCount;
            }
        }
        return shardAllocationList;
    }

    @Override
    public String getPodUrl(long clusterId, long configVersionId, String podName) {
        MetadataClusterData clusterData = bmrMetadataService.queryClusterDetail(clusterId);
        String appId = clusterData.getAppId();
        PaasConfig paasConfig = buildPaasConfig(configVersionId);
        String[] appIdSplit = appId.split("\\.");

        String url = UrlBuilder.ofHttp("https://caster.bilibili.co/webconsole")
                .addQuery("scheduler", "k8s")
                .addQuery("platform", "caster")
                .addQuery("namespace", appIdSplit[0] + Constants.LINE + appIdSplit[1] + Constants.LINE + paasConfig.getEnv())
                .addQuery("cluster", Constants.CK_K8S_CLUSTER)
                .addQuery("pod", podName)
                .addQuery("container", "clickhouse")
                .build();
        return url;
    }

    @Override
    public String queryPodLog(long clusterId, long configVersionId, String podName, int limit) {
        MetadataClusterData clusterData = bmrMetadataService.queryClusterDetail(clusterId);
        PaasConfig paasConfig = buildPaasConfig(configVersionId);
        return casterService.queryLog(podName, clusterData.getAppId(), paasConfig.getEnv(), Constants.CK_K8S_CLUSTER, limit, "clickhouse");
    }

    @Override
    public void updateShardFile(Long componentId, List<ClickhouseCluster> clusterList) {
        for (ClickhouseCluster ckCluster : clusterList) {
            CKClusterType clusterType = ckCluster.getClusterType();
            List<Shards> shardList = ckCluster.getLayout().getShards();
            String shardsStr = ObjectTransferUtils.yamlObjectToStr(shardList);
            switch (clusterType) {
                case ADMIN:
                    bmrConfigService.coverSpecialFileContext(componentId, Constants.ADMIN_SHARDS_FILE, shardsStr);
                    break;
                case REPLICA:
                    bmrConfigService.coverSpecialFileContext(componentId, Constants.REPLICA_SHARDS_FILE, shardsStr);
                    break;
            }
        }
    }

    /**
     * 将原始列表中的每个元素拆分成多个1，并将这些1添加到结果列表中
     * 此方法的目的是将给定的整数列表中的每个元素转换为列表中相同数量的1
     * 例如，给定列表[2, 3]将被转换为[1, 1, 1, 1, 1]
     * 如果中间有0，则shard会有0并跳过
     * 如[2,0,3]转换[1, 1, 0, 1, 1, 1]
     *
     * @param originalList 原始整数列表，其中的每个元素将被拆分成多个1
     * @return 包含拆分后1的结果列表
     */
    private List<Integer> splitList(List<Integer> originalList) {
        List<Integer> resultList = new ArrayList<>();
        for (Integer num : originalList) {
            if (num.equals(0)) {
                resultList.add(0);
            } else {
                for (int i = 0; i < num; i++) {
                    resultList.add(1);
                }
            }
        }
        return resultList;
    }

    /**
     * 扩容clickhouse的容器，
     * shards中包含容器replica
     * 如果集群是 replica类型，则根据输入的shardAllocationList划分shard和容器，如List<2,2,3>就代表3个shards，其中replica数分别为2,2,3
     * 如果集群是admin类型，则以1,1,1,1,1,1,1的形式，1个shard中只有1个replica，1的总和等于shardAllocation数量的总和，如List<2,2,3>代表7个1即7个shards
     *
     * @param configVersionId     配置版本ID，用于标识配置版本
     * @param podTemplate         pod模板，用于创建新的shard或replica
     * @param shardAllocationList shard分配列表，指定了每个shard及其期望的replica数量 List<2,2,3></>
     * @param clickhouseCluster   Clickhouse集群对象，包含当前集群的布局信息
     * @param configVersionId
     * @param podTemplate
     * @param shardAllocationList
     * @param clickhouseCluster
     * @return 返回更新后的shards列表
     * @return
     */
    private List<Shards> scaleShards(long configVersionId, String podTemplate, List<Integer> shardAllocationList,
                                     ClickhouseCluster clickhouseCluster, String propsFileName) {
        Map<String, Shards> shardNameToShardMap = clickhouseCluster
                .getLayout()
                .getShards()
                .stream().collect(Collectors.toMap(Shards::getName, Function.identity()));
        ShardsProps shardsProps = buildShardProps(configVersionId, propsFileName);
        List<Shards> newShardList = new LinkedList<>();
        int replicaId = 0;
        for (int i = 0; i < shardAllocationList.size(); i++) {
            int shardId = i;
            Integer requireReplicaCount = shardAllocationList.get(i);
//            如果replica数量为0，则跳过，不创建新的replica，replica的id也会加1
            if (requireReplicaCount == 0) {
                ++replicaId;
                continue;
            }

            String shardName = Constants.SHARD + shardId;
            Shards shards = shardNameToShardMap.computeIfAbsent(shardName, name -> {
                Shards newShards = new Shards();
                newShards.setReplicas(new ArrayList<>());
                return newShards;
            });
            shards.setName(shardName);
            shards.setReplicasCount(requireReplicaCount);

            List<Replica> newReplicaList = new LinkedList<>();
            List<Replica> replicaList = shards.getReplicas();
            for (int j = 0; j < requireReplicaCount; j++) {
                String replicaName = String.format("%02d", ++replicaId);
                Replica replica;
                if (j < replicaList.size()) {
                    replica = replicaList.get(j);
                    replica.getTemplates().setPodTemplate(podTemplate);
                } else {
                    replica = new Replica();
                    ShardTemplate shardTemplate = new ShardTemplate();
                    shardTemplate.setPodTemplate(podTemplate);
                    shardTemplate.setDataVolumeClaimTemplate(shardsProps.getDataVolumeClaimTemplate());
                    shardTemplate.setReplicaServiceTemplate(shardsProps.getReplicaServiceTemplate());
                    replica.setTemplates(shardTemplate);
                }
                replica.setName(replicaName);
                newReplicaList.add(replica);
            }
            shards.setReplicas(newReplicaList);
            newShardList.add(shards);
        }
        return newShardList;
    }

    /**
     * 构建clickhouse生成ftl文件的对象
     *
     * @param configVersionId
     * @return
     */
    @Override
    public ClickhouseDeployDTO buildClickhouseDeployDTO(long configVersionId) {

        Assert.isTrue(configVersionId > 0, "配置版本不能小于0");

        ConfigGroupRelationEntity defaultGroupRelation = bmrConfigService.queryDefaultGroupRelation(configVersionId);
        Assert.isTrue(!Objects.isNull(defaultGroupRelation), "不存在默认配置组");

        ClickhouseDeployDTO clickhouseDeployDTO = new ClickhouseDeployDTO();

        /**
         * 配置常规属性
         */
        PaasConfig paasConfig = buildPaasConfig(configVersionId);
        String env = paasConfig.getEnv();

        ConfigDetailData configDetailData = bmrConfigService.queryConfigDetailById(configVersionId);
        Long componentId = configDetailData.getComponentId();
        MetadataComponentData componentData = bmrMetadataService.queryComponentByComponentId(componentId);
        MetadataClusterData clusterData = bmrMetadataService.queryClusterDetail(componentData.getClusterId());
        String appId = clusterData.getAppId();
        Assert.isTrue(StringUtils.hasText(appId), "集群未配置appId");

        clickhouseDeployDTO.setAppId(appId);
        clickhouseDeployDTO.setEnv(env);
        clickhouseDeployDTO.setClusterName(Constants.CK_K8S_CLUSTER);
        clickhouseDeployDTO.setResourcePoolName(paasConfig.getResourcePoolName());

        ChTemplate chTemplate = buildChTemplate(configVersionId);
        clickhouseDeployDTO.setChTemplate(chTemplate);

        ChConfig chConfig = buildChConfig(configVersionId);
        clickhouseDeployDTO.setChConfig(chConfig);

        return clickhouseDeployDTO;
    }

    /**
     * 配置chTemplate
     */
    private ChTemplate buildChTemplate(long configVersionId) {
        ChTemplate chTemplate = new ChTemplate();
        List<TemplatePodTemplate> podTemplateList = buildPodTemplate(configVersionId);
        chTemplate.setPodTemplates(podTemplateList);

        FileServiceTemplate serviceTemplate = buildServiceTemplate(configVersionId);
        chTemplate.setService(serviceTemplate);

        List<TemplateVolumeTemplate> volumeTemplateList = buildVolumeTemplateList(configVersionId);
        chTemplate.setVolumeTemplate(volumeTemplateList);
        return chTemplate;
    }

    /**
     * 配置chConfig
     */
    private ChConfig buildChConfig(long configVersionId) {
        ChConfig chConfig = new ChConfig();
        Map<String, String> settingMap = buildObjByMapFile(configVersionId, Constants.CONFIG_SETTINGS_FILE, ConfigVersionType.NORMAL, Function.identity());
        chConfig.setSettings(settingMap);

        ChConfig.ZookeeperDTO zookeeperDTO = new ChConfig.ZookeeperDTO();
        List<ZookeeperResource> zookeeperList = buildObjByYamlFile(configVersionId, Constants.ZOOKEEPER_FILE, new TypeReference<List<ZookeeperResource>>() {
        }, ConfigVersionType.NORMAL, Function.identity());
        zookeeperDTO.setNodes(zookeeperList);
        chConfig.setZookeeper(zookeeperDTO);

        Map<String, String> profileMap = buildObjByMapFile(configVersionId, Constants.PROFILES_FILE, ConfigVersionType.NORMAL, Function.identity());
        chConfig.setProfiles(profileMap);

        Map<String, Object> userMap = buildUserMap(configVersionId);
        chConfig.setUsers(userMap);

        ChConfig.FileDTO fileDTO = new ChConfig.FileDTO();
        String storage = buildObjByStringFile(configVersionId, Constants.STORAGE_FILE, ConfigVersionType.NORMAL, Function.identity());
        fileDTO.setStorage(storage);
        chConfig.setFiles(fileDTO);

        /**
         * 配置 ClickhouseCluster
         */
        ClickhouseCluster replicaCluster = buildCkCluster(configVersionId, CKClusterType.REPLICA);
        ClickhouseCluster adminCluster = buildCkCluster(configVersionId, CKClusterType.ADMIN);
        if (Objects.isNull(adminCluster)) {
            throw new RuntimeException(String.format("%s 配置文件不能为空", Constants.ADMIN_SHARDS_FILE));
        }

        List<ClickhouseCluster> ckClusterList = new ArrayList<>();
        if (!Objects.isNull(replicaCluster)) {
            ckClusterList.add(replicaCluster);
        }
        ckClusterList.add(adminCluster);
        chConfig.setClusters(ckClusterList);
        return chConfig;
    }

    /**
     * 配置 ClickhouseCluster
     */
    @Override
    public ClickhouseCluster buildCkCluster(long configVersionId, CKClusterType clusterType) {
        String shardsFileName = clusterType.getShardsFileName();
        String shardsPropsFileName = clusterType.getShardsPropsFileName();
        List<Shards> shardList = buildShards(configVersionId, shardsFileName);

        if (CollectionUtils.isEmpty(shardList)) {
            return null;
        }

        ClickhouseCluster clickhouseCluster = new ClickhouseCluster();
        ShardsProps shardProps = buildShardProps(configVersionId, shardsPropsFileName);

        ClickhouseCluster.SecretDTO secretDTO = new ClickhouseCluster.SecretDTO();
        secretDTO.setValue(shardProps.getSecret());
        clickhouseCluster.setSecret(secretDTO);

        ClickhouseCluster.LayoutDTO layoutDTO = new ClickhouseCluster.LayoutDTO();
        layoutDTO.setShards(shardList);
        clickhouseCluster.setLayout(layoutDTO);

        TemplateSchemaPolicy templateSchemaPolicy = new TemplateSchemaPolicy();
        templateSchemaPolicy.setReplica(shardProps.getSchemaPolicyReplica());
        templateSchemaPolicy.setShard(shardProps.getSchemaPolicyShard());
        clickhouseCluster.setSchemaPolicy(templateSchemaPolicy);

        clickhouseCluster.setName(shardProps.getName());
        clickhouseCluster.setClusterType(clusterType);
        return clickhouseCluster;
    }

    @Override
    public List<PodTemplateDTO> queryPodTemplateListByClusterId(long clusterId) {
        Assert.isTrue(clusterId > 0, "clusterId不能为空");
        List<MetadataComponentData> metadataComponentList = bmrMetadataService.queryComponentListByClusterId(clusterId);
        MetadataComponentData ckComponent = metadataComponentList.stream()
                .filter(component -> Constants.CLICK_HOUSE_COMPONENT.equals(component.getComponentName()))
                .findAny()
                .orElse(null);
        Assert.isTrue(!Objects.isNull(ckComponent), "集群中不存在clickhouse组件,集群id" + clusterId);
        int ckComponentId = ckComponent.getId();
        ConfigDetailData configDetailData = bmrConfigService.queryConfigDetailByComponentId(ckComponentId);
        Assert.isTrue(!Objects.isNull(configDetailData), "集群中clickhouse组件的配置文件不存在,组件id" + ckComponentId);
        return queryPodTemplateList(configDetailData.getId());
    }

    /**
     * 解析cluster_admin_shards.yaml和cluster_replica_shards.yaml
     *
     * @param configVersionId
     * @param shardsFile
     * @return
     */
    private List<Shards> buildShards(long configVersionId, String shardsFile) {
        return buildObjByYamlFile(configVersionId, shardsFile, new TypeReference<List<Shards>>() {
        }, ConfigVersionType.SPECIAL, Function.identity());
    }

    /**
     * 解析cluster_replica_props.xml和cluster_admin_props.xml
     *
     * @param configVersionId
     * @param shardsPropsFile
     * @return
     */
    private ShardsProps buildShardProps(long configVersionId, String shardsPropsFile) {
        return buildObjByMapFile(configVersionId, shardsPropsFile, ConfigVersionType.NORMAL,
                map -> BeanUtil.mapToBean(map, ShardsProps.class, false, CopyOptions.create()));
    }

    /**
     * 如果usermap中如果有default/networks/ip属性，则该属性是一个yaml列表。其他属性都为字符串
     *
     * @param configVersionId
     * @return
     */
    private Map<String, Object> buildUserMap(long configVersionId) {
        return buildObjByMapFile(configVersionId, Constants.USERS_FILE, ConfigVersionType.NORMAL,
                originMap -> {

                    Map<String, Object> resultMap = new HashMap<>();
                    for (Map.Entry<String, String> entry : originMap.entrySet()) {

                        String key = entry.getKey();
                        String value = entry.getValue();

                        if (!Constants.CLICKHOUSE_USERS_IP.equals(key)) {
                            resultMap.put(key, value);
                            continue;
                        }

                        try {
                            List<String> ipList = ObjectTransferUtils.yamlToObject(value, new TypeReference<List<String>>() {
                            });
                            resultMap.put(key, ipList);
                        } catch (Exception e) {
                            throw new RuntimeException("handle user.xml ip error, error message is " + e.getMessage(), e);
                        }
                    }

                    return resultMap;
                });
    }

    private PaasConfig buildPaasConfig(long configVersionId) {
        PaasConfig paasConfig = buildObjByMapFile(configVersionId, Constants.PASS_CONFIG_FILE, ConfigVersionType.NORMAL,
                map -> {
                    return BeanUtil.mapToBean(map, PaasConfig.class, false, CopyOptions.create());
                });
        return paasConfig;
    }

    private List<TemplateVolumeTemplate> buildVolumeTemplateList(long configVersionId) {
        List<TemplateVolumeTemplate> volumeTemplateList = buildObjByYamlFile(configVersionId, Constants.VOLUME_TEMPLATES_FILE, new TypeReference<List<FileVolumeTemplate>>() {
        }, ConfigVersionType.NORMAL, configVolumeTemplateList -> {
            LinkedList<TemplateVolumeTemplate> templateVolumeTemplateList = new LinkedList<>();

            for (FileVolumeTemplate configVolumeTemplate : configVolumeTemplateList) {
                TemplateVolumeTemplate templateVolumeTemplate = new TemplateVolumeTemplate();
                templateVolumeTemplate.setName(configVolumeTemplate.getName());

                ConfigVolumeSpec spec = configVolumeTemplate.getSpec();
                templateVolumeTemplate.setStorageClass(spec.getStorageClassName());

                TemplateVolumeResource templateVolumeResource = new TemplateVolumeResource();
                templateVolumeResource.setStorage(spec.getResources().getRequests().getStorage());
                templateVolumeTemplate.setResources(templateVolumeResource);
                templateVolumeTemplateList.add(templateVolumeTemplate);
            }

            return templateVolumeTemplateList;
        });
        return volumeTemplateList;
    }

    /**
     * 根据普通字符串文件构建对象，以字符串形式解析
     *
     * @param configVersionId 配置版本ID，用于指定配置数据的版本
     * @param fileName        文件名，指定要查询的配置文件
     * @param function        函数，用于将字符串形式的配置数据转换为目标对象
     * @param <T>             目标对象的类型
     * @return 转换后的目标对象
     * @throws RuntimeException 如果在转换过程中发生错误，抛出运行时异常
     */
    private <T> T buildObjByStringFile(long configVersionId, String fileName, ConfigVersionType fileType, Function<String, T> function) {
        try {
            ConfigData configData = getConfigData(configVersionId, fileName, fileType, ConfigFileTypeEnum.STRING);
            String context = configData.getContext();
            return function.apply(context);
        } catch (Exception e) {
            String errorMsg = String.format("transfer file error, file is %s, error is %s", fileName, e.getMessage());
            log.error(errorMsg, e);
            throw new RuntimeException(errorMsg);
        }
    }

    private ConfigData getConfigData(long configVersionId, String fileName, ConfigVersionType fileType, ConfigFileTypeEnum fileFormat) {
        ConfigData configData;
        if (ConfigVersionType.SPECIAL.equals(fileType)) {
            ConfigDetailData configDetailData = bmrConfigService.queryConfigDetailById(configVersionId);
            Long componentId = configDetailData.getComponentId();
            configData = bmrConfigService.queryItemListAndData(null, componentId, fileName, fileFormat, fileType);
        } else {
            configData = bmrConfigService.queryItemListAndData(configVersionId, null, fileName, fileFormat, fileType);
        }
        return configData;
    }

    /**
     * 根据YAML文件构建一个对象
     *
     * @param configVersionId 配置版本ID，用于指定配置的版本
     * @param fileName        文件名，用于指定YAML文件
     * @param typeReference   类型引用，用于指定要构建的对象类型
     * @param function        转换函数，用于对构建的对象进行进一步处理
     * @param <T>             要构建的对象的类型
     * @param <R>             转换函数的返回类型
     * @return 返回构建的对象
     * @throws 如果文件转换为yaml时发生异常，则抛出RuntimeException
     */
    private <T, R> R buildObjByYamlFile(long configVersionId, String fileName, TypeReference<T> typeReference, ConfigVersionType fileType, Function<T, R> function) {
//        将yaml字符串转换成T
        Function<String, R> composedFunction = yaml -> {
            try {
                if (StringUtils.isEmpty(yaml)) {
                    return null;
                }
                T t = ObjectTransferUtils.yamlToObject(yaml, typeReference);
                return function.apply(t);
            } catch (Exception e) {
                throw new RuntimeException("文件转换成yaml时异常,文件名" + fileName, e);
            }
        };
        return buildObjByStringFile(configVersionId, fileName, fileType, composedFunction);
    }

    /**
     * 根据普通字符串文件构建对象，以inputstream形式解析
     *
     * @param configVersionId 配置版本ID，用于指定配置数据的版本
     * @param fileName        文件名，指定要查询的配置文件
     * @param function        函数，用于将字符串形式的配置数据转换为目标对象
     * @param <T>             目标对象的类型
     * @return 转换后的目标对象
     * @throws RuntimeException 如果在转换过程中发生错误，抛出运行时异常
     */
    private <T> T buildObjByNormalInputStreamFile(long configVersionId, String fileName, ConfigVersionType fileType, Function<InputStream, T> function) {
        Function<String, T> composedFunction = context -> {
            try (ByteArrayInputStream inputStream = new ByteArrayInputStream(context.getBytes())) {
                return function.apply(inputStream);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };

        return buildObjByStringFile(configVersionId, fileName, fileType, composedFunction);
    }

    /**
     * 根据普通map文件构建对象
     *
     * @param configVersionId 配置版本ID，用于指定配置数据的版本
     * @param fileName        文件名，指定要查询的配置文件
     * @param function        函数，用于将字符串形式的配置数据转换为目标对象
     * @param <T>             目标对象的类型
     * @return 转换后的目标对象
     * @throws RuntimeException 如果在转换过程中发生错误，抛出运行时异常
     */
    private <T> T buildObjByMapFile(long configVersionId, String fileName, ConfigVersionType fileType, Function<Map<String, String>, T> function) {
        try {
            ConfigData configData = getConfigData(configVersionId, fileName, fileType, ConfigFileTypeEnum.MAP);
            List<ConfigItemInfo> itemList = configData.getItems();
            Map<String, String> map = itemList.stream().collect(Collectors.toMap(ConfigItemInfo::getItemKey, ConfigItemInfo::getItemValue));
            return function.apply(map);
        } catch (Exception e) {
            log.error("transfer file error, file is " + fileName, e);
            throw new RuntimeException(e);
        }
    }

    private FileServiceTemplate buildServiceTemplate(long configVersionId) {
        return buildObjByYamlFile(configVersionId, Constants.SERVICE_TEMPLATES_FILE, new TypeReference<FileServiceTemplate>() {
        }, ConfigVersionType.NORMAL, Function.identity());
    }

    private List<TemplatePodTemplate> buildPodTemplate(long configVersionId) {
        List<TemplatePodTemplate> templatePodTemplateList = buildObjByYamlFile(configVersionId, Constants.POD_TEMPLATES_FILE, new TypeReference<List<FilePodTemplate>>() {
        }, ConfigVersionType.NORMAL, configPodTemplateList -> {
            List<TemplatePodTemplate> podTemplateList = new LinkedList<>();
            for (FilePodTemplate configPodTemplate : configPodTemplateList) {
                TemplatePodTemplate templatePodTemplate = new TemplatePodTemplate();

                templatePodTemplate.setName(configPodTemplate.getName());
                ConfigContainer configContainer = configPodTemplate.getSpec().getContainer();

                ConfigContainer.ContainerResource configContainerResource = configContainer.getResources();
                ConfigContainerResource resourceLimit = configContainerResource.getLimits();
                ConfigContainerResource resourceRequest = configContainerResource.getRequests();
                TemplateContainerResource templateHardwareResource = new TemplateContainerResource();
                templateHardwareResource.setCpuLimit(convertCpu(resourceLimit.getCpu()));
                templateHardwareResource.setCpuReq(convertCpu(resourceRequest.getCpu()));
                templateHardwareResource.setMemLimit(convertMemory(resourceLimit.getMemory()));
                templateHardwareResource.setMemReq(convertMemory(resourceRequest.getMemory()));
                templatePodTemplate.setResources(templateHardwareResource);

                templatePodTemplate.setImage(configContainer.getImage());
                templatePodTemplate.setDnsConfig(configPodTemplate.getSpec().getDnsConfig());
                podTemplateList.add(templatePodTemplate);
            }
            return podTemplateList;
        });
        return templatePodTemplateList;
    }

    /**
     * 解析memory
     *
     * @param input
     * @return
     */
    private int convertMemory(String input) {
        String unit;
        int value;

        Matcher matcher = memoryPattern.matcher(input);

        if (matcher.matches()) {
            value = Integer.parseInt(matcher.group(1));
            unit = matcher.group(2);
        } else {
            throw new IllegalArgumentException("Invalid input: " + input);
        }

        int result;
        if (unit.equals("M")) {
            result = value;
        } else if (unit.equals("Gi")) {
            result = value * 1024;
        } else {
            throw new IllegalArgumentException("Invalid unit: " + unit);
        }

        return result;
    }

    /**
     * 解析cpu
     *
     * @param input
     * @return
     */
    protected int convertCpu(String input) {
        if (input.endsWith("m")) {
            // 如果参数以 "m" 结尾，则移除 "m" 并将字符串转换为整数
            String numericValue = input.substring(0, input.length() - 1);
            return Integer.parseInt(numericValue);
        } else {
            // 如果参数不以 "m" 结尾，则将字符串转换为整数并乘以 1000
            return Integer.parseInt(input) * 1000;
        }
    }
}
