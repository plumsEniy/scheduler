package com.bilibili.cluster.scheduler.common.dto.bmr.resourceV2.model;

import com.bilibili.cluster.scheduler.common.dto.bmr.resourceV2.HostType;
import lombok.Data;

import java.time.LocalDateTime;


@Data
public class ResourceHostInfo {

    /**
     * 自增主键ID
     */
    private Long id;

    /**
     * 主机名称
     */
    private String hostName;

    /**
     * 主机所在机架
     */
    private String rack = "NaN";

    /**
     * 主机IP
     */
    private String ip;

    /*
     * SSD硬盘数量
     */
    private Integer numSsd = 0;

    /**
     * SATA硬盘数量
     */
    private Integer numSata = 0;

    /**
     * CPU核数
     */
    private Integer numCpuCore = 0;

    /**
     * 内存总量
     */
    private Long memTotal = 0L;

    /**
     * nvme的数量
     */
    private Integer nvmeTotal = 0;

    /**
     * job执行方式
     */
    private String jobExecuteType = "NaN";

    /**
     * 初始化状态
     */
    private String initState;

    /**
     * job_agent状态
     * NORMAL,
     * NOT_INSTALL,
     * LOW_VERSION;
     */
    private String jobAgentState = "NaN";
    ;

    /**
     * 资产编号
     */
    private String bs;


    /**
     * 主机类型
     */
    private HostType hostType;

    /**
     * 磁盘信息
     */
    private String diskInfo = "NaN";

    /**
     * 内存信息
     */
    private String memoryInfo = "NaN";

    /**
     * CPU信息
     */
    private String cpuInfo = "NaN";

    /**
     * 创建者
     */
    private String creator;

    /**
     * 更新者
     */
    private String updater;

    /**
     * 创建时间
     */
    private LocalDateTime ctime;

    /**
     * 修改时间
     */
    private LocalDateTime mtime;

    /**
     * 是否删除
     */
    private Integer deleted = 0;

    /***
     *  部门
     * */
    private String department = "NaN";

    /**
     * 机房
     */
    private String idc = "NaN";

    /**
     * 区域
     */
    private String zone = "NaN";

    /**
     * 套餐信息
     */
    private String suit = "NaN";

    private String ssdAbstract = "NaN";

    private String hhdAbstract = "NaN";

    private String nvmeAbstract = "NaN";

}
