package com.bilibili.cluster.scheduler.common.dto.bmr.resource.req;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import java.util.List;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateNodeLabelReq {

    @NotBlank(message = "label is blank")
    private String label;

    @NotEmpty(message = "node list is empty")
    private List<String> nodes;

    private Long clusterId;
}
