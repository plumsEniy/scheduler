package com.bilibili.cluster.scheduler.common.dto.jobAgent;

import cn.hutool.core.annotation.Alias;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class TaskSetData {
    private long id;
    @Alias("flow_id")
    private long flowId;
    @Alias("set_name")
    private String setName;
    @Alias("set_type")
    private int setType;
    @Alias("script_id")
    private int scriptId;
    private String args;
    @Alias("exec_dir")
    private String execDir;
    @Alias("exec_user")
    private String execUser;
    private int timeout;
    @Alias("is_ssh")
    private boolean isSsh;
    @Alias("is_strict")
    private boolean isStrict;
    @Alias("callback_url")
    private String callbackUrl;
    @Alias("is_use_global_host")
    private boolean isUseGlobalHost;
    private int state;
    @Alias("batch_num")
    private int batchNum;
    @Alias("created_by")
    private String createdBy;
    @Alias("modified_by")
    private String modifiedBy;
    @Alias("started_by")
    private String startedBy;
    private String stime;
    private String etime;
    private String ctime;
    private String mtime;
    private Map<String, String> env;
    private List<String> hosts;
    @Alias("task_type")
    private int taskType;
    @Alias("script_name")
    private String scriptName;
    @Alias("script_type")
    private int scriptType;
    private String content;
}