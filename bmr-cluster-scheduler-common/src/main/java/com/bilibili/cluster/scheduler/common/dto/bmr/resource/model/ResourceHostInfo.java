package com.bilibili.cluster.scheduler.common.dto.bmr.resource.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * @description:
 * @Date: 2024/5/23 09:01
 * @Author: nizhiqiang
 */

@Data
public class ResourceHostInfo {

    @ApiModelProperty("自增主键ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @ApiModelProperty("主机所在机架")
    private String rack;

    @ApiModelProperty("主机名称")
    private String hostName;

    @ApiModelProperty("主机IP")
    private String ip;

    @ApiModelProperty("主机状态，关机/维修中/重启中")
    private String hostState;

    @ApiModelProperty("磁盘信息")
    private String diskInfo;

    @ApiModelProperty("内存信息")
    private String memoryInfo;

    @ApiModelProperty("CPU信息")
    private String cpuInfo;

    @ApiModelProperty("归属部门")
    private String department;

    @ApiModelProperty("创建者")
    private String creator;

    @ApiModelProperty("更新者")
    private String updater;

    @ApiModelProperty("创建时间")
    private String ctime;

    @ApiModelProperty("修改时间")
    private String mtime;

    @ApiModelProperty("是否删除")
    @TableLogic
    private Integer deleted;

    @ApiModelProperty("SSD硬盘数量")
    private Integer numSsd;

    @ApiModelProperty("SATA硬盘数量")
    private Integer numSata;

    @ApiModelProperty("CPU核数")
    private Integer numCpuCore;

    @ApiModelProperty("初始化状态")
    private String initState;

    @ApiModelProperty("job_agent状态")
    private String jobAgentState;
    @ApiModelProperty("job_agent执行方式")
    private String jobExecuteType;

    @ApiModelProperty("job_agent排序位")
    private Integer jobAgentOrder;

    @ApiModelProperty("初始化流程id")
    private String schedInstanceId;

    @ApiModelProperty("初始化工作流项目id")
    private String initProjectId;

    @ApiModelProperty("资产编号")
    private String bs;

    @ApiModelProperty("是否属于BMR平台托管,0不属于，1属于")
    private Integer bmrFlag;

    @ApiModelProperty("套餐信息")
    private String suit;

    @TableField(exist = false)
    private List<String> componentList = new ArrayList<>();

    @TableField(exist = false)
    private List<String> planComponentList = new ArrayList<>();


    private Long memTotal;
    @ApiModelProperty("机房")
    private String idc;

    private String ssdAbstract;

    private String hhdAbstract;
    private String nvmeAbstract;
    private Integer nvmeTotal;

    @ApiModelProperty("是否开启超线程")
    private Boolean isHyperThreading;
    @ApiModelProperty("内核版本")
    private String kernelVersion;
    @ApiModelProperty("是否在混部集群")
    private Boolean isDocker;

    private String hddDisc;

    private String nvmeDisc;

    /**
     * 开始时间
     */
    private String startTime;

    /**
     * 结束时间
     */
    private String endTime;
    /**
     * 页码
     */
    private int pageNum = 1;
    /**
     * 每页条数
     */
    private int pageSize = 10;
}
