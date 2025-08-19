package com.bilibili.cluster.scheduler.common.dto.scheduler.resp;


import com.bilibili.cluster.scheduler.common.dto.scheduler.model.ProjectDefine;
import lombok.Data;

import java.util.List;

@Data
public class ProjectResp {

    private int code;

    private String msg;

    private List<ProjectDefine> data;
}
