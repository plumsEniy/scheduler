package com.bilibili.cluster.scheduler.common.dto.jobAgent;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.List;
import java.util.Map;

/**
 * @description: 脚本执行信息
 * @Date: 2023/9/8 10:35
 * @Author: xiexieliangjie
 */

@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class JobScriptInfo {

    private Long id;
    private Integer flow_id;
    private String set_name;
    private Integer set_type;
    private Long script_id;
    private String tmp_script_uid;
    private String args;
    private String exec_dir;
    private String exec_user;
    private Integer timeout;
    private Boolean is_ssh;
    private Boolean is_strict;
    private String callback_url;
    private Boolean is_usr_global_host;
    private Integer state;
    private Integer batch_num;
    private String created_by;
    private String modified_by;
    private String started_by;
    private Boolean is_notified;
    private String stime;
    private String etime;
    private String ctime;
    private String mtime;
    private String notify_to;
    private Map<String, String> env;
    private List<String> hosts;
    private Integer task_type;
    private Map<String, String> host_env_map;
    private String script_name;
    private Integer script_type;
    private String content;

}
