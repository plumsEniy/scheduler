package com.bilibili.cluster.scheduler.common.dto.caster.resp;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DeployK8sResp extends BaseComResp {
    private K8sRespData data;
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public class K8sRespData {
        private String template;

    }
}
