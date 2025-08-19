package com.bilibili.cluster.scheduler.common.dto.bmr.resource.req;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.http.HttpMethod;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import java.util.List;

/**
 * @description: yarn的标签移除
 * @Date: 2024/3/11 19:29
 * @Author: nizhiqiang
 */
@Data
@AllArgsConstructor
public class RemoveYarnLabelReq {

    private String label;
    private List<String> nodes;

    private Long clusterId;
}
