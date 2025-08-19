package com.bilibili.cluster.scheduler.api.service.scheduler.experiment;

import cn.hutool.core.net.url.UrlBuilder;
import cn.hutool.http.HttpRequest;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.alibaba.fastjson.JSON;
import com.bilibili.cluster.scheduler.common.Constants;
import com.bilibili.cluster.scheduler.common.dto.bmr.experiment.ExperimentType;
import com.bilibili.cluster.scheduler.common.dto.flow.req.DeployOneFlowReq;
import com.bilibili.cluster.scheduler.common.dto.presto.experiment.TrinoClusterInfo;
import com.bilibili.cluster.scheduler.common.dto.presto.experiment.TrinoExperimentExtFlowParams;
import com.bilibili.cluster.scheduler.common.dto.scheduler.resp.TasksExecDetailResp;
import com.bilibili.cluster.scheduler.common.dto.bmr.experiment.ExperimentJobType;
import com.bilibili.cluster.scheduler.common.dto.spark.params.SparkExperimentFlowExtParams;
import com.bilibili.cluster.scheduler.common.dto.spark.params.SparkVersionLockExtParams;
import com.bilibili.cluster.scheduler.common.dto.spark.plus.SparkManagerJob;
import com.bilibili.cluster.scheduler.common.dto.spark.plus.req.CreateExperimentRequest;
import com.bilibili.cluster.scheduler.common.enums.flow.FlowDeployType;
import com.bilibili.cluster.scheduler.common.enums.flow.FlowEffectiveModeEnum;
import com.bilibili.cluster.scheduler.common.enums.flow.FlowGroupTypeEnum;
import com.bilibili.cluster.scheduler.common.enums.flow.FlowReleaseScopeType;
import com.bilibili.cluster.scheduler.common.utils.BaseRespUtil;
import com.bilibili.cluster.scheduler.common.utils.LocalDateFormatterUtils;
import org.junit.Test;
import org.slf4j.MDC;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ExperimentCITestUnit {


    @Test
    public void testCreateExperimentReq() {

        final CreateExperimentRequest request = new CreateExperimentRequest();
        request.setUser("xuchen");
        request.setDescription("spark-manager单路实验任务");
        request.setWorkflowName("spark-manager单路实验任务");
        request.setPlatformA("spark_3.1");

        Map<String, String> confAMap = new HashMap<>();
        confAMap.put("spark.docker.imageName", "hub.bilibili.co/compile/spark-internal:client-1.1.20-rc7");

        request.setConfA(JSONUtil.toJsonStr(confAMap));

        request.setMetrics("CPU,MEMORY,DURATION");

        final SparkManagerJob job = new SparkManagerJob();
        job.setJobId("9201837-1");
        job.setBusinessTime(LocalDateFormatterUtils.getNowMilliFmt());
        job.setCodeA("drop table if exists tmp_bdp.rods_db3295_deploy_execution_flow_a_hr_spark_v3_41319069117317120;\n" +
                "\n" +
                "create table tmp_bdp.rods_db3295_deploy_execution_flow_a_hr_spark_v3_41319069117317120 like b_ods.rods_db3295_deploy_execution_flow_a_hr TBLPROPERTIES ('table.retention.period'='14');\n" +
                "\n" +
                "insert overwrite table tmp_bdp.rods_db3295_deploy_execution_flow_a_hr_spark_v3_41319069117317120 partition (log_date = '20240925', log_hour = '19') (`id`,`cluster_id`,`component_id`,`role_name`,`component_name`,`cluster_name`,`deploy_type`,`release_scope_type`,`deploy_package_type`,`restart`,`effective_mode`,`sched_project_id`,`sched_pipeline_id`,`resource_node_group`,`submit_user`,`approver`,`remark`,`deploy_env`,`order_no`,`order_id`,`apply_state`,`parallelism`,`package_id`,`config_id`,`tolerance`,`cur_fault`,`start_time`,`end_time`,`flow_state`,`ctime`,`mtime`,`op_strategy`,`log_id`,`deleted`,`config_group`,`queue_id`,`queue_effective_name`,`latest_active_time`,`cur_batch_id`,`group_type`,`incident_transfer_uuid`,`extra_env`,`approve_uuid`,`job_execute_type`) select  cast(`id` as bigint) `id`,cast(`cluster_id` as bigint) `cluster_id`,cast(`component_id` as string) `component_id`,`role_name`,b_shielder_mask_all_v1(`component_name`) `component_name`,`cluster_name`,`deploy_type`,`release_scope_type`,`deploy_package_type`,`restart`,`effective_mode`,`sched_project_id`,b_shielder_mask_all_v1(`sched_pipeline_id`) `sched_pipeline_id`,cast(`resource_node_group` as bigint) `resource_node_group`,`submit_user`,`approver`,`remark`,`deploy_env`,`order_no`,`order_id`,`apply_state`,`parallelism`,cast(`package_id` as bigint) `package_id`,cast(`config_id` as bigint) `config_id`,`tolerance`,`cur_fault`,`start_time`,`end_time`,`flow_state`,`ctime`,`mtime`,`op_strategy`,cast(`log_id` as bigint) `log_id`,`deleted`,cast(`config_group` as bigint) `config_group`,cast(`queue_id` as bigint) `queue_id`,`queue_effective_name`,`latest_active_time`,`cur_batch_id`,`group_type`,`incident_transfer_uuid`,`extra_env`,`approve_uuid`,`job_execute_type` from  b_ods.rods_db3295_deploy_execution_flow_a_hr_iceberg_snapshot;");
        job.setTargetTableA("tmp_bdp.rods_db3295_deploy_execution_flow_a_hr_spark_v3_41319069117317120");
        request.setJobs(JSONUtil.toJsonStr(Arrays.asList(job)));

        System.out.println(JSONUtil.toJsonStr(request));


        request.setPlatformB("spark_4.0");
        Map<String, Object> confBMap = new HashMap<>();
        confBMap.put("spark.docker.imageName", "hub.bilibili.co/compile/spark-internal:v3.1.1-bilibili-1.1.22-rc6");
        request.setConfB(JSONUtil.toJsonStr(confBMap));

        job.setCodeB("drop table if exists tmp_bdp.rods_db3295_deploy_execution_flow_a_hr_spark_v4_41319069117317120;\n" +
                "\n" +
                "create table tmp_bdp.rods_db3295_deploy_execution_flow_a_hr_spark_v4_41319069117317120 like b_ods.rods_db3295_deploy_execution_flow_a_hr TBLPROPERTIES ('table.retention.period'='14');\n" +
                "\n" +
                "insert overwrite table tmp_bdp.rods_db3295_deploy_execution_flow_a_hr_spark_v4_41319069117317120 partition (log_date = '20240925', log_hour = '19') (`id`,`cluster_id`,`component_id`,`role_name`,`component_name`,`cluster_name`,`deploy_type`,`release_scope_type`,`deploy_package_type`,`restart`,`effective_mode`,`sched_project_id`,`sched_pipeline_id`,`resource_node_group`,`submit_user`,`approver`,`remark`,`deploy_env`,`order_no`,`order_id`,`apply_state`,`parallelism`,`package_id`,`config_id`,`tolerance`,`cur_fault`,`start_time`,`end_time`,`flow_state`,`ctime`,`mtime`,`op_strategy`,`log_id`,`deleted`,`config_group`,`queue_id`,`queue_effective_name`,`latest_active_time`,`cur_batch_id`,`group_type`,`incident_transfer_uuid`,`extra_env`,`approve_uuid`,`job_execute_type`) select  cast(`id` as bigint) `id`,cast(`cluster_id` as bigint) `cluster_id`,cast(`component_id` as string) `component_id`,`role_name`,b_shielder_mask_all_v1(`component_name`) `component_name`,`cluster_name`,`deploy_type`,`release_scope_type`,`deploy_package_type`,`restart`,`effective_mode`,`sched_project_id`,b_shielder_mask_all_v1(`sched_pipeline_id`) `sched_pipeline_id`,cast(`resource_node_group` as bigint) `resource_node_group`,`submit_user`,`approver`,`remark`,`deploy_env`,`order_no`,`order_id`,`apply_state`,`parallelism`,cast(`package_id` as bigint) `package_id`,cast(`config_id` as bigint) `config_id`,`tolerance`,`cur_fault`,`start_time`,`end_time`,`flow_state`,`ctime`,`mtime`,`op_strategy`,cast(`log_id` as bigint) `log_id`,`deleted`,cast(`config_group` as bigint) `config_group`,cast(`queue_id` as bigint) `queue_id`,`queue_effective_name`,`latest_active_time`,`cur_batch_id`,`group_type`,`incident_transfer_uuid`,`extra_env`,`approve_uuid`,`job_execute_type` from  b_ods.rods_db3295_deploy_execution_flow_a_hr_iceberg_snapshot;");
        job.setTargetTableB("tmp_bdp.rods_db3295_deploy_execution_flow_a_hr_spark_v4_41319069117317120");
        request.setJobs(JSONUtil.toJsonStr(Arrays.asList(job)));
        request.setDescription("spark-manager AB版本对比实验任务");
        request.setWorkflowName("spark-manager双路实验任务");
        request.setMetrics("COUNT,CRC32,CPU,MEMORY,DURATION");
        job.setJobId("9201837-3");

        System.out.println(JSONUtil.toJsonPrettyStr(request));
    }



    @Test
    public void testCreateExperimentFlowReq() {
        String baseJson = "{\n" +
                "  \"approver\": \"nizhiqiang,liuguohui\",\n" +
                "  \"cluster\": \"Spark\",\n" +
                "  \"clusterName\": \"Spark\",\n" +
                "  \"componentName\": \"Spark\",\n" +
                "  \"configGroup\": 0,\n" +
                "  \"configId\": 0,\n" +
                "  \"deployPackageType\": \"SERVICE_PACKAGE\",\n" +
                "  \"deployType\": \"SPARK_EXPERIMENT\",\n" +
                "  \"effectiveMode\": \"RESTART_EFFECTIVE\",\n" +
                "  \"executeType\": \"EXECUTE_NOW\",\n" +
                "  \"extParams\": \"\",\n" +
                "  \n" +
                "  \"groupType\": \"RANDOM_GROUP\",\n" +
                "  \"isApproval\": \"false\",\n" +
                "  \"nodeGroup\": 0,\n" +
                "  \"nodeList\": [\n" +
                "    \"\"\n" +
                "  ],\n" +
                "  \"packageId\": 0,\n" +
                "  \"parallelism\": 2,\n" +
                "  \"releaseScopeType\": \"GRAY_RELEASE\",\n" +
                "  \"remark\": \"spark实验任务\",\n" +
                "  \"roleName\": \"spark\",\n" +
                "  \"tolerance\": 1,\n" +
                "  \"userName\": \"xuchen\"\n" +
                "}";
        JSONObject jsonObject = JSONUtil.parseObj(baseJson);
        List<String> nodeList = new ArrayList<>();
        nodeList.add("d2ede2052e7e40748fee3d794064c06d");
        nodeList.add("eea8fbba8e0a4f63945856eb6fcc61c1");

        jsonObject.set("nodeList", nodeList);
        final SparkExperimentFlowExtParams experimentFlowExtParams = new SparkExperimentFlowExtParams();
        String platformA = "spark_4.0.1-preview";
        // String imageA = "hub.bilibili.co/datacenter-spark/spark-build:cad40a2f1afec384f164826547112618ff51d0e4-nyx";
        // String imageA = "hub.bilibili.co/compile/spark-internal:v3.1.1-bilibili-1.1.21-rc9";
        String imageA = "hub.bilibili.co/datacenter-spark/spark-build:3b0de230c430882285ab79732046a3a24f186b50-nyx";
        experimentFlowExtParams.setPlatformA(platformA);
        experimentFlowExtParams.setImageA(imageA);
        experimentFlowExtParams.setExperimentType(ExperimentType.PERFORMANCE_TEST);
        experimentFlowExtParams.setJobType(ExperimentJobType.TEST_JOB);

        Map<String, Object> confAMap = new HashMap<>();
        confAMap.put("spark.yarn.tags", "dcinfo=jscs,NOHYBRID"); // 集群队列
        confAMap.put("spark.yarn.queue", "report.adhoc"); // 额外参数

        experimentFlowExtParams.setConfA(JSONUtil.toJsonStr(confAMap));
        experimentFlowExtParams.setTestSetVersionId(6);
        experimentFlowExtParams.setMetrics("CPU,MEMORY,DURATION");
        jsonObject.set("extParams", JSONUtil.toJsonStr(experimentFlowExtParams));

        System.out.println(JSONUtil.toJsonStr(jsonObject));

    }


    @Test
    public void testCreateSparkVersionLockFlowReq() {
        String baseJson = "{\n" +
                "  \"approver\": \"nizhiqiang,liuguohui\",\n" +
                "  \"cluster\": \"Spark\",\n" +
                "  \"clusterId\": null,\n" +
                "  \"clusterName\": \"Spark\",\n" +
                "  \"componentId\": 0,\n" +
                "  \"componentName\": \"Spark\",\n" +
                "  \"configGroup\": 0,\n" +
                "  \"configId\": 0,\n" +
                "  \"deployPackageType\": \"SERVICE_PACKAGE\",\n" +
                "  \"deployType\": \"SPARK_VERSION_RELEASE\",\n" +
                "  \"effectiveMode\": \"RESTART_EFFECTIVE\",\n" +
                "  \"executeType\": \"EXECUTE_NOW\",\n" +
                "  \"extParams\": \"\",\n" +
                "  \n" +
                "  \"groupType\": \"RANDOM_GROUP\",\n" +
                "  \"isApproval\": \"false\",\n" +
                "  \"nodeGroup\": 0,\n" +
                "  \"nodeList\": [\n" +
                "    \"\"\n" +
                "  ],\n" +
                "  \"packageId\": 0,\n" +
                "  \"parallelism\": 2,\n" +
                "  \"releaseScopeType\": \"GRAY_RELEASE\",\n" +
                "  \"remark\": \"spark实验任务\",\n" +
                "  \"roleName\": \"spark\",\n" +
                "  \"tolerance\": 1,\n" +
                "  \"userName\": \"xuchen\"\n" +
                "}";
        JSONObject jsonObject = JSONUtil.parseObj(baseJson);
        List<String> nodeList = new ArrayList<>();
        nodeList.add("9410983");
        nodeList.add("9410985");

        nodeList.add("9406560");
        nodeList.add("9404325");

        jsonObject.set("nodeList", nodeList);

        final SparkVersionLockExtParams versionLockExtParams = new SparkVersionLockExtParams();
        versionLockExtParams.setApproverList(Arrays.asList("nizhiqiang", "liuguohui", "xuchen"));
        versionLockExtParams.setRemark("spark版本锁定测试11");
        versionLockExtParams.setTitle("spark-manager");

        jsonObject.set("extParams", JSONUtil.toJsonStr(versionLockExtParams));

        System.out.println(JSONUtil.toJsonStr(jsonObject));

    }

    @Test
    public void testRegexMatch() {
        String workflowInstanceUrl = "http://pre-bmr.scheduler.bilibili.co/#/projects/12969915754848/workflow/instances/41215";
        String regexPattern = ".*\\/projects\\/(?<projectId>\\w+)\\/workflow\\/instances\\/(?<instanceId>\\w+)";

        Pattern urlRegexPattern = Pattern.compile(regexPattern);
        Matcher matcher = urlRegexPattern.matcher(workflowInstanceUrl);
        String projectId = null;
        String instanceId = null;

        if (matcher.matches()) {
            projectId = matcher.group("projectId");
            instanceId = matcher.group("instanceId");
        } else {
            System.out.println("NOT MATCH");
        }

        System.out.println("projectId is: " + projectId);
        System.out.println("instanceId is: " + instanceId);
    }

    @Test
    public void testQueryTaskInstanceDetail() {

        String projectId = "12969915754848";
        String instanceId = "41215";

        String BASE_URL = "http://pre-bmr.scheduler.bilibili.co/dolphinscheduler";

        String url = UrlBuilder.ofHttp(BASE_URL)
                .addPath(String.format("/projects/%s/process-instances/%s/tasks", projectId, instanceId))
                .build();
        MDC.put(Constants.DOLPHIN_SCHEDULER_TOKEN_KEY, "8fca65db29266ca3ab29e734ff281d4b");

        String token = MDC.get(Constants.DOLPHIN_SCHEDULER_TOKEN_KEY);
        System.out.println("query task exec state url is " +  url);
        String respStr = HttpRequest.get(url)
                .header(Constants.TOKEN, token)
                .execute().body();

        TasksExecDetailResp resp = JSONUtil.toBean(respStr, TasksExecDetailResp.class);
        BaseRespUtil.checkDolphinSchedulerResp(resp);

        System.out.println(JSONUtil.toJsonStr(resp.getData().getTaskList()));
    }



    @Test
    public void testCreateTrinoExperimentReq() {
        final CreateExperimentRequest request = new CreateExperimentRequest();
        request.setUser("xuchen");
        request.setDescription("trino-manager单路实验任务");
        request.setWorkflowName("trino-manager单路实验");
        request.setPlatformA("presto_a");
        request.setPlatformB("empty_default");

        Map<String, Object> prestoConf = new HashMap<>();
        prestoConf.put("trino.client.tag", "ab_test_1");
        request.setConfA(JSONUtil.toJsonStr(prestoConf));

        request.setMetrics("CPU,MEMORY,DURATION");
        final SparkManagerJob job = new SparkManagerJob();
        job.setJobId("test-presto-34928279956");
        job.setBusinessTime(LocalDateFormatterUtils.getNowMilliFmt());

        final StringBuilder sqlBuilder = new StringBuilder();
        sqlBuilder.append("drop table if exists tmp_bdp.select_default_table_name_trino_1_48587313965170688;\n");
        sqlBuilder.append("CREATE TABLE tmp_bdp.select_default_table_name_trino_1_48587313965170688 AS with data as\n(\nselect \n  season_id,\n  season_type,\n  epid,\n  avid,\n  src,\n  creative_id,\n  title,\n  subtitle,\n  reason,\n  cover,\n  gifcover,\n  imp,\n  case when click-dislike*5 > 0 then click-dislike*5 else 3 end as click,\n  dislike,\n  log_date,\n  log_hour,\n  material_strategy_type\nfrom \n  ai.ogv_creative_resources_all\nwhere log_date='20250804' and log_hour='14' and log_type='v3'\nand src not in ('movie')\nand position('精英上班族×普通宅女高中生的恋爱喜剧就此开幕！' in title) = 0\nand position('魔眼收集列车2/6 神威之车轮' in title) = 0\nand position('娶了又纯又欲的老婆' in title) = 0\nand position('罗小黑战记今日起周更' in title) = 0\nand position('创造者史蒂芬·海伦伯格去世' in title) = 0\nand position('剧情风骚，男主装逼，神曲众多，甚至还有三角恋' in title) = 0\nand position('俄罗斯黄段子医院' in title) = 0\nand position('俊子唱歌变好听了，还翻牌UP主二创了' in title) = 0\nand position('这是我能看内容!?gakki老婆出浴暴露绝美曲线!' in title) = 0\nand position('爷青结！当年那个点读机女孩，你猜现在怎么样了？' in title) = 0\nand position('【西游伏妖篇】星爷监制吴亦凡主演' in title) = 0\nand position('一起蹦迪！吴亦凡rap现场，这段有点燃' in title) = 0\nand position('信息量巨大！五年前节目颜王怒喷小猪' in title) = 0\nand position('笑到肚子抽筋的神对话！男人帮围攻小猪问八卦' in title) = 0\nand position('王迅到底有多欧！像极了我闭眼考试还得了90分的样！' in title) = 0\nand position('王迅到底有多欧！像极了我闭眼考试还得了90分的亚子！' in title) = 0\nand position('孙红雷罗志祥辣眼女装，这个...视觉冲击太吓人' in title) = 0\nand position('惊艳！罗志祥扮丑为G.E.M.邓紫棋打歌' in title) = 0\nand position('灵笼终章，勇士启航' in title) = 0\nand position('【秘密花园】美少年被宠坏，10年没下过床' in title) = 0\nand position('除了小S，原来是她能让蔡康永闭嘴？！' in title) = 0\nand position('张哲瀚' in title) = 0\nand position('母亲寻找儿子50年，最后发现对方竟埋在院里' in title) = 0\nand position('西虹市首富王多鱼“入驻”B站！' in title) = 0\nand position('《信条》导演诺兰经典作，豆瓣9.3必看' in title) = 0\nand (position('邓超胸口纹个喜洋洋这么嚣张，吴京看懵了' in title) = 0 or epid = 388348)\nand epid not in (393470,411182)\nand epid not in (\n270346, 121248, 121249, 251603, 472757, 257325, 249567, 249568, 249569, 256615, 120520, 240486, 240487,\n240488, 240489, 240490, 240491, 205413, 119045, 251844, 358298, 423406, 334372, 335842, 335268, 253498,\n279722, 328460, 338402, 171804, 459896)\nand epid not in (\n395180, 391795, 375403, 375402, 375401, 375400, 375399, 375398, 375397, 375396, 375395, 341321, 341320,\n341319, 341318, 341317, 341316, 341315, 341314, 341313, 341312, 341311, 341310, 341309, 414629, 414628,\n414627, 414626, 414625, 414624, 414623, 414622, 414621, 414620, 414619, 414618, 414617, 414616, 414615,\n414614, 414613, 414612, 414611, 414610, 414609, 414608, 414607, 414606)\nand cover not in (\n    'http://i0.hdslb.com/bfs/archive/60ef5fdb2aeb1b60b1aae8d49caab5bc70b4a4b9.png',\n    'http://i0.hdslb.com/bfs/feed-admin/408b7dd7f9811ab4a6508b23c6ca1de2f4810c78.png',\n    'http://i0.hdslb.com/bfs/bangumi/image/269b5b35a57b64ffd8676c47fd65b8d2c13fc3a8.jpg',\n    'http://i0.hdslb.com/bfs/archive/8cc30611fc487698572b2943f071cf745b0acd53.jpg',\n    'http://i0.hdslb.com/bfs/archive/97e0c53c53edd7fc8cc523523b18ca4a8086ba63.png',\n    'http://i0.hdslb.com/bfs/archive/52db5bfdac5e55deb3267e8a1f227caf10c5f50b.png',\n    'http://i0.hdslb.com/bfs/archive/f7f9098d7a16bacb301b3da2295597ce9e14d306.jpg',\n    'http://i0.hdslb.com/bfs/archive/6000a4b62f34bed079e73417fd2e7abe8777d4b7.png',\n    'http://i0.hdslb.com/bfs/archive/62722c732ff641e73da0aa475f11f2fb117d35fd.png',\n    'http://i0.hdslb.com/bfs/archive/411b42035766bd1acbe58b83f558a2ad7e28ad5e.jpg',\n    'http://i0.hdslb.com/bfs/archive/d8e55afa66d195eac6d64f1566e4da32d6d7cea5.jpg',\n    'http://i0.hdslb.com/bfs/archive/135aeee4b6060cb07628c5ca58dca3d1b0351055.jpg',\n    'http://i0.hdslb.com/bfs/archive/c7c791a627e47a5d8258821dd816912068edeb9a.png',\n    'http://i0.hdslb.com/bfs/feed-admin/e7cf7710530584d5fd2256ad4b322cdcf44bb4f3.png',\n    'http://i0.hdslb.com/bfs/archive/d79575da05860459274b1267b227a84bc5049293.jpg',\n    'http://i0.hdslb.com/bfs/bangumi/image/6ca1d331d780bccd29583f80d11ba3d54566f03b.png',\n    'http://i0.hdslb.com/bfs/feed-admin/3f749e32f174c7d8b99ee2ab2dcaf3ebe93987ca.png')\n\ngroup by 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17\n),\ndata_cnt as (\n  select avid, epid, count(*) as cnt from data group by 1, 2\n)\n\nselect \n  t1.*\nfrom \n  data t1\njoin\n  (select * from data_cnt where cnt > 1) t2\non (t1.avid=t2.avid and t1.epid=t2.epid)\norder by t1.avid, t1.epid, src, title, cover, creative_id;\n");
        job.setCodeA(sqlBuilder.toString());
        job.setTargetTableA("tmp_bdp.select_default_table_name_trino_1_48587313965170688");
        List<SparkManagerJob> jobs = Arrays.asList(job);
        request.setJobs(JSONUtil.toJsonStr(jobs));
        request.setDetails("trino-manager experiment task");

        System.out.println(JSONUtil.toJsonStr(request));
    }

    @Test
    public void testGenerateTrinoExperimentFlowReq() {
        List<String> jobs = new ArrayList<>();
        jobs.add("76fb0e3fd6734b70b5142a0a1acb5433");

        DeployOneFlowReq flowReq = new DeployOneFlowReq();
        flowReq.setParallelism(1);
        flowReq.setTolerance(jobs.size());
        flowReq.setDeployType(FlowDeployType.TRINO_EXPERIMENT);
        flowReq.setReleaseScopeType(FlowReleaseScopeType.GRAY_RELEASE.name());
        flowReq.setRemark("Trino对比实验任务");
        flowReq.setRestart(false);
        flowReq.setGroupType(FlowGroupTypeEnum.RANDOM_GROUP);
        flowReq.setUserName("xuchen");
        flowReq.setIsApproval("false");

        TrinoExperimentExtFlowParams params = new TrinoExperimentExtFlowParams();
        params.setExperimentType(ExperimentType.PERFORMANCE_TEST);
        params.setPlatformA("presto_a");
        params.setImageA("hub.bilibili.co/datacenter-trino/trino-build-400:v400-bili-0.6.3-nyx");
        params.setTestSetVersionId(325l);
        params.setInstanceId(22l);

        Map<String, Object> confAMap = new HashMap<>();
        confAMap.put("trino.client.tag", "ab_test_1");
        params.setConfA(JSONUtil.toJsonStr(confAMap));

        final TrinoClusterInfo aClusterInfo = new TrinoClusterInfo();
        aClusterInfo.setClusterId(56l);
        aClusterInfo.setComponentId(84l);
        aClusterInfo.setConfigId(2901l);
        aClusterInfo.setPackId(681l);
        aClusterInfo.setRebuildCluster(false);
        params.setARunTimeConf(JSONUtil.toJsonStr(aClusterInfo));

        flowReq.setExtParams(JSON.toJSONString(params));
        flowReq.setComponentName("Trino");
        flowReq.setRoleName("Trino");
        flowReq.setClusterName("Trino");
        flowReq.setEffectiveMode(FlowEffectiveModeEnum.IMMEDIATE_EFFECTIVE);
        flowReq.setNodeList(jobs);

        System.out.println(JSONUtil.toJsonStr(flowReq));
    }


}
