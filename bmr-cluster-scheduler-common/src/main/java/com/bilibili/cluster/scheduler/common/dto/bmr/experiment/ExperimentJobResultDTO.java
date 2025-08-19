package com.bilibili.cluster.scheduler.common.dto.bmr.experiment;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;


@Data
public class ExperimentJobResultDTO {

    @NotNull(message = "测试实例id不能为空")
    @ApiModelProperty(value="测试实例id")
    private Long ciInstanceId;

    /**
     * 任务配置id
     **/
    @ApiModelProperty(value="任务配置id")
    private Long jobConfId;

    /**
     * 执行流程id
     **/
    @ApiModelProperty(value="执行流程id")
    private Long flowId;

    /**
     * 执行任务id
     **/
    @NotNull(message = "执行任务id不能null")
    @ApiModelProperty(value="执行任务id")
    private Long execNodeId;

    /**
     * manager任务运行id
     **/
    @NotNull(message = "manager任务运行id不能null")
    @ApiModelProperty(value="manager任务运行id")
    private String jobId;

    /**
     * job运行详情
     **/
    @ApiModelProperty(value="job运行详情")
    private String jobDetail;

    /**
     * 数据质量验证（是否通过，条数&crc）
     **/
    @ApiModelProperty(value="数据质量验证（是否通过，条数&crc）")
    private String dqcResultType;

    /**
     * 测试1开始时间
     **/
    @NotNull(message = "测试1开始时间不能null")
    @ApiModelProperty(value="测试1开始时间")
    private LocalDateTime aStartTime;

    /**
     * 测试1结束时间
     **/
    @ApiModelProperty(value="测试1结束时间")
    private LocalDateTime aEndTime;

    /**
     * 测试1运行时间
     **/
    @ApiModelProperty(value="测试1运行时间")
    private Double aRunTime;

    /**
     * 测试1资源cpu使用大小
     **/
    @ApiModelProperty(value="测试1资源cpu使用大小")
    private Double aResourceCpuUsage;

    /**
     * 测试1资源内存使用大小
     **/
    @ApiModelProperty(value="测试1资源内存使用大小")
    private Double aResourceMemUsage;

    /**
     * 测试2开始时间
     **/
    @ApiModelProperty(value="测试2开始时间")
    private LocalDateTime bStartTime;

    /**
     * 测试2结束时间
     **/
    @ApiModelProperty(value="测试2结束时间")
    private LocalDateTime bEndTime;

    /**
     * 测试2运行时间
     **/
    @ApiModelProperty(value="测试2运行时间")
    private Double bRunTime;

    /**
     * 测试2资源cpu使用大小
     **/
    @ApiModelProperty(value="测试2资源cpu使用大小")
    private Double bResourceCpuUsage;

    /**
     * 测试2资源内存使用大小
     **/
    @ApiModelProperty(value="测试2资源内存使用大小")
    private Double bResourceMemUsage;

    /**
     * 创建时间
     **/
    @ApiModelProperty(value="创建时间")
    private LocalDateTime ctime;

    /**
     * 更新时间
     **/
    @ApiModelProperty(value="更新时间")
    private LocalDateTime mtime;

    /**
     * 任务执行状态
      */
    private ExperimentJobStatus jobStatus;

    /**
     * 实验A appId
     */
    private String aApplicationId;

    /**
     * 实验B appId
     */
    private String bApplicationId;

}
