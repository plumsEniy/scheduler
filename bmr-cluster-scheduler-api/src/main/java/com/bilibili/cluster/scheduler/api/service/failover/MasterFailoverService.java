package com.bilibili.cluster.scheduler.api.service.failover;

import cn.hutool.json.JSONUtil;
import com.bilibili.cluster.scheduler.api.configuration.MasterConfig;
import com.bilibili.cluster.scheduler.api.dto.registry.NodeType;
import com.bilibili.cluster.scheduler.api.dto.registry.Server;
import com.bilibili.cluster.scheduler.api.redis.RedissonLockSupport;
import com.bilibili.cluster.scheduler.api.registry.service.impl.RegistryClient;
import com.bilibili.cluster.scheduler.api.scheduler.cache.ProcessInstanceExecCacheManager;
import com.bilibili.cluster.scheduler.api.service.flow.ExecutionNodeService;
import com.bilibili.cluster.scheduler.common.Constants;
import com.bilibili.cluster.scheduler.common.entity.ExecutionFlowEntity;
import com.bilibili.cluster.scheduler.common.enums.flow.FlowStatusEnum;
import com.bilibili.cluster.scheduler.common.utils.NetUtils;
import com.bilibili.cluster.scheduler.dao.mapper.ExecutionFlowMapper;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@Slf4j
public class MasterFailoverService {

    @Value("${spring.profiles.active}")
    private String active;

    @Resource
    private ExecutionFlowMapper executionFlowMapper;

    @Resource
    private ProcessInstanceExecCacheManager processInstanceExecCacheManager;

    @Resource
    private ExecutionNodeService executionNodeService;

    private final RegistryClient registryClient;

    private final MasterConfig masterConfig;
    private final String localAddress;


    public MasterFailoverService(@NonNull RegistryClient registryClient, @NonNull MasterConfig masterConfig) {
        this.registryClient = registryClient;
        this.masterConfig = masterConfig;
        this.localAddress = NetUtils.getAddr(masterConfig.getListenPort());

    }

    /**
     * check master failover
     */
    public void checkMasterFailover() {
        List<String> needFailoverMasterHosts = executionFlowMapper.queryNeedFailoverFlowHost().stream()
                // failover myself || dead server
                .filter(host -> localAddress.equals(host) || !registryClient.checkNodeExists(host, NodeType.MASTER)).distinct().collect(Collectors.toList());
        if (CollectionUtils.isEmpty(needFailoverMasterHosts)) {
            return;
        }
        log.info("Master failover service {} begin to failover hosts:{}", localAddress, needFailoverMasterHosts);

        for (String needFailoverMasterHost : needFailoverMasterHosts) {
            failoverMaster(needFailoverMasterHost);
        }

    }

    public void failoverMaster(String masterHost) {
        String failoverPath = Constants.REGISTRY_DTS_LOCK_FAILOVER_MASTERS + "/" + masterHost;
        try {
            registryClient.getLock(failoverPath);
            doFailoverMaster(masterHost);
        } catch (Exception e) {
            log.error("Master server failover failed, host:{}", masterHost, e);
        } finally {
            registryClient.releaseLock(failoverPath);
        }
    }

    /**
     * Failover master, will failover process instance and associated task instance.
     * <p>When the process instance belongs to the given masterHost and the restartTime is before the current server start up time,
     * then the process instance will be failovered.
     *
     * @param masterHost master host
     */
    private void doFailoverMaster(@NonNull String masterHost) {
        log.info("start recovery flow job.....env:{}", active);
        StopWatch failoverTimeCost = StopWatch.createStarted();

        Optional<Date> masterStartupTimeOptional = getServerStartupTime(registryClient.getServerList(NodeType.MASTER), masterHost);
        // 根据要恢复的主机实例. 查询待恢复flow, 除结单状态以外
        List<ExecutionFlowEntity> needFailoverFlowList = executionFlowMapper.queryNeedFailoverFlow(masterHost);
        if (CollectionUtils.isEmpty(needFailoverFlowList)) {
            return;
        }

        log.info("Master[{}] failover starting there are {} workflowInstance may need to failover, will do a deep check, workflowInstanceIds: {}", masterHost, needFailoverFlowList.size(), needFailoverFlowList.stream().map(ExecutionFlowEntity::getId).collect(Collectors.toList()));
        try {
            for (ExecutionFlowEntity executionFlowEntity : needFailoverFlowList) {
                Long flowId = executionFlowEntity.getId();
                log.info("WorkflowInstance failover starting, flowId:{}, env:{}", flowId, active);
                if (!checkFlowNeedFailover(masterStartupTimeOptional, executionFlowEntity)) {
                    log.info("WorkflowInstance doesn't need to failover, processInstance:{}, env:{}", JSONUtil.toJsonStr(executionFlowEntity), active);
                    continue;
                }

                if (!checkFlowJobNeedFailover(executionFlowEntity.getFlowStatus())) {
                    log.info("Workflow id {} process status is {}  doesn't need to failover, env:{}", executionFlowEntity.getId(), executionFlowEntity.getFlowStatus(), active);
                    continue;
                }

                log.info("update recovery flow hostName:{}, flowId:{}, env:{}", masterHost, flowId, active);
                executionFlowMapper.updateFlowHostName(NetUtils.getAddr(masterConfig.getListenPort()), flowId);
                executionNodeService.recoveryFlowNode(executionFlowEntity);
                log.info("WorkflowInstance failover finished, env:{}", active);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }

        failoverTimeCost.stop();
        log.info("Master[{}] failover finished, useTime:{}ms, env:{}", masterHost, failoverTimeCost.getTime(TimeUnit.MILLISECONDS), active);
    }

    private Optional<Date> getServerStartupTime(List<Server> servers, String host) {
        if (CollectionUtils.isEmpty(servers)) {
            return Optional.empty();
        }
        Date serverStartupTime = null;
        for (Server server : servers) {
            if (host.equals(server.getHost() + Constants.COLON + server.getPort())) {
                serverStartupTime = server.getCreateTime();
                break;
            }
        }
        return Optional.ofNullable(serverStartupTime);
    }


    private boolean checkFlowNeedFailover(Optional<Date> beFailoveredMasterStartupTimeOptional, @NonNull ExecutionFlowEntity executionFlow) {
        // The process has already been failover, since when we do master failover we will hold a lock, so we can
        // guarantee
        // the host will not be set concurrent.
        if (Constants.NULL.equals(executionFlow.getHostName())) {
            return false;
        }

        if (!beFailoveredMasterStartupTimeOptional.isPresent()) {
            // the master is not active, we can failover all it's processInstance
            return true;
        }

        Date beFailoveredMasterStartupTime = beFailoveredMasterStartupTimeOptional.get();

        if (Date.from(executionFlow.getCtime().atZone(ZoneId.systemDefault()).toInstant()).after(beFailoveredMasterStartupTime)) {
            // The processInstance is newly created
            return false;
        }

        LocalDateTime latestActiveTime = executionFlow.getLatestActiveTime();
        String hostName = executionFlow.getHostName();
        if (NetUtils.getAddr(masterConfig.getListenPort()).equals(hostName)) {
            if (Date.from(latestActiveTime.atZone(ZoneId.systemDefault()).toInstant()).after(beFailoveredMasterStartupTime)) {
                log.warn("flow id {} has been recoveried, will ignore...", executionFlow.getId());
                return false;
            }
        }


        if (processInstanceExecCacheManager.contains(executionFlow.getId())) {
            // the processInstance is a running process instance in the current master
            return false;
        }

        return true;
    }

    private boolean checkFlowJobNeedFailover(FlowStatusEnum flowStatusEnum) {
        if (flowStatusEnum.equals(FlowStatusEnum.CANCEL) || flowStatusEnum.equals(FlowStatusEnum.TERMINATE)) {
            return false;
        }
        if (flowStatusEnum.name().contains("FAIL_") || flowStatusEnum.name().contains("SUCCEED_")) {
            return false;
        }
        return true;
    }

}
