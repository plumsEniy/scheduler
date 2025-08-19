package com.bilibili.cluster.scheduler.common.dto.bmr.metadata.req;

import com.bilibili.cluster.scheduler.common.dto.bmr.metadata.enums.InstallationPackageType;
import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class QueryInstallationPackageListReq {

    private Long clusterId;

    /**
     * 组件id，优先组件id，没有组件id使用组件名
     */
    private Long componentId;

    private String componentName;

    /**
     * 是否为测试
     */
    private Boolean isTest;

    /**
     * 大版本
     */
    private String seniorVersion;

    /**
     * 小版本
     */
    private String minorVersion;

    /**
     * 安装包名/tag  支持模糊查询
     */
    private String tagName;
    /**
     * 交付状态 枚举类型
     */
    private String deliveryStatus;
    /**
     * 发布状态 枚举类型
     */
    private String releaseStatus;
    /**
     * ci分支   支持模糊
     */
    private String ciBranch;

    /**
     * commit id  支持模糊
     */
    private String commitId;
    /**
     * 产物包名称   支持模糊
     */
    private String productBagName;
    /**
     * 创建人  支持模糊查询
     */
    private String founder;

    private InstallationPackageType packageType;


    private Integer pageNum = 1;

    private Integer pageSize = 10;
}
