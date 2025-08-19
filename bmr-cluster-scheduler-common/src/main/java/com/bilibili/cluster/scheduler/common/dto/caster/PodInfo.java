package com.bilibili.cluster.scheduler.common.dto.caster;

import cn.hutool.core.annotation.Alias;
import com.bilibili.cluster.scheduler.common.Constants;
import com.bilibili.cluster.scheduler.common.dto.caster.dto.ContainerStatus;
import lombok.Data;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * @description: pod信息
 * @Date: 2024/7/22 11:09
 * @Author: nizhiqiang
 */

@Data
public class PodInfo {

    private String name;

    @Alias("pod_ip")
    private String podIp;

    @Alias("owner_name")
    private String ownerName;

    private String status;

    @Alias("host_ip")
    private String hostIp;

    private String hostname;

    private String image;

    private String namespace;

    private LocalDateTime time;

    private Map<String, String> labels;

    @Alias("container_status")
    private List<ContainerStatus> containerStatusList;

    /**
     * 当前pod为running且容器为ready才判定为成功
     *
     * @return
     */
    public String getPodStatus() {
        String currentPodStatus = StringUtils.isEmpty(this.getStatus()) ? Constants.UNKNOWN : this.getStatus();
        if (Constants.POD_STATUS_RUNNING.equals(currentPodStatus)) {
            List<ContainerStatus> containerStatusList = this.getContainerStatusList();
            if (!CollectionUtils.isEmpty(containerStatusList)) {

                for (ContainerStatus containerStatus : containerStatusList) {

                    Boolean ready = containerStatus.getReady();
                    if (!ready) {
                        return currentPodStatus;
                    }
                }
                currentPodStatus = Constants.POD_STATUS_SUCCESS;
            }
        }
        return currentPodStatus;
    }

    public String getPodType() {
        if (name.contains(Constants.WORKER)) {
            return Constants.WORKER;
        }
        if (name.contains(Constants.RESOURCE)) {
            return Constants.RESOURCE;
        }
        if (name.contains(Constants.COORDINATOR)) {
            return Constants.COORDINATOR;
        }
        return Constants.EMPTY_STRING;
    }

}
