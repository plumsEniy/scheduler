package com.bilibili.cluster.scheduler.api.dto.registry;

import lombok.Data;

import java.util.Date;

@Data
public class Server {

    private int id;

    private String host;

    private int port;

    private String zkDirectory;

    /**
     * resource info: CPU and memory
     */
    private String resInfo;

    private Date createTime;

    private Date lastHeartbeatTime;

}
