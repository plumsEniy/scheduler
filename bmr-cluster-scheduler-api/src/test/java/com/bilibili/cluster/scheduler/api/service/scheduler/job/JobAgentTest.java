package com.bilibili.cluster.scheduler.api.service.scheduler.job;

import cn.hutool.core.lang.UUID;
import cn.hutool.system.JavaInfo;
import cn.hutool.system.JvmInfo;
import cn.hutool.system.UserInfo;
import com.bilibili.cluster.scheduler.api.service.jobAgent.JobAgentServiceImpl;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class JobAgentTest {

    @Test
    public void test01() {
        final JobAgentServiceImpl jobAgentService = new JobAgentServiceImpl();
        List<String> hostList = new ArrayList<>();
        hostList.add("jscs-bigdata-test-31");
        hostList.add("jscs-bigdata-test-33");
        hostList.add("jscs-bigdata-test-35");

        Map<String, Boolean> liveStatus = jobAgentService.queryNodeJobAgentLiveStatus(hostList);

        System.out.println(liveStatus);


        System.out.println(new JvmInfo());

        System.out.println(new JavaInfo());

        System.out.println(new UserInfo());



    }

    @Test
    public void test02() {
        String uuid = UUID.fastUUID().toString();
        System.out.println(uuid.replace("-", ""));
        System.out.println(uuid.replace("-", "").length());
    }

    @Test
    public void test03() {
        String value = " [{\"SPARK_CLIENT_PACK_DOWNLOAD_URL\":\"http://jssz-boss.bilibili.co/nyx-artifacts/artifact/datacenter.spark.spark-build_f834b790bd5e52fdff14ce0357c8c12a93404b7f_1730982189460659-normal.tar.gz\",\"SPARK_CLIENT_PACK_NAME\":\"Spark-4.0.0.1\",\"SPARK_CLIENT_PACK_MD5\":\"21ac66b7ea204acfcd9292d29b506fee\",\"SPARK_CLIENT_TYPE\":\"SPARK\"},{\"SPARK_CLIENT_PACK_DOWNLOAD_URL\":\"http://jssz-boss.bilibili.co/nyx-artifacts/artifact/datacenter.spark.spark-build_ed57ef1dd910962dfabff6361c19985dd515cf70_1730972766514063-normal.tar.gz\",\"SPARK_CLIENT_PACK_NAME\":\"Spark-4.0.0\",\"SPARK_CLIENT_PACK_MD5\":\"48960d16801d2988122e418ca1f0073c\",\"SPARK_CLIENT_TYPE\":\"SPARK\"}]";
        value = "[{\"SPARK_CLIENT_PACK_DOWNLOAD_URL\":\"http://jssz-boss.bilibili.co/nyx-artifacts/artifact/datacenter.bmr-doctor.data-service_1821e504dfa79d48b6f932d60085df6b62362cda_1731323080565654-normal.tar.gz\",\"SPARK_CLIENT_PACK_NAME\":\"ONE-CLIENT-1.4\",\"SPARK_CLIENT_PACK_MD5\":\"7871df780fe95c83097426510cedc253\",\"SPARK_CLIENT_TYPE\":\"ONE-CLIENT\"},{\"SPARK_CLIENT_PACK_DOWNLOAD_URL\":\"http://jssz-boss.bilibili.co/nyx-artifacts/artifact/datacenter.bmr-doctor.data-service_bde5010cd246bdecb0e3b80a60fdb49c695d709e_1731311182421072-normal.tar.gz\",\"SPARK_CLIENT_PACK_NAME\":\"ONE-CLIENT-1.3\",\"SPARK_CLIENT_PACK_MD5\":\"e65e18070bc1538e419141ff27bb7184\",\"SPARK_CLIENT_TYPE\":\"ONE-CLIENT\"},{\"SPARK_CLIENT_PACK_DOWNLOAD_URL\":\"http://jssz-boss.bilibili.co/nyx-artifacts/artifact/datacenter.bmr-doctor.data-service_597a9ec45b1ed6eae2a2e0e2219e596cbe392572_1731310447597198-normal.tar.gz\",\"SPARK_CLIENT_PACK_NAME\":\"ONE-CLIENT-1.2\",\"SPARK_CLIENT_PACK_MD5\":\"768d6397b4319b029be830a1596c7b23\",\"SPARK_CLIENT_TYPE\":\"ONE-CLIENT\"},{\"SPARK_CLIENT_PACK_DOWNLOAD_URL\":\"http://jssz-boss.bilibili.co/nyx-artifacts/artifact/datacenter.bmr-doctor.data-service_c4ddef202395fa4b127cebf19fddbadb593f7f9f_1731057578818405-normal.tar.gz\",\"SPARK_CLIENT_PACK_NAME\":\"ONE-CLIENT-1.1\",\"SPARK_CLIENT_PACK_MD5\":\"d877d703f49be508fb3205a09ad4e747\",\"SPARK_CLIENT_TYPE\":\"ONE-CLIENT\"}]";
        System.out.println(value.length());
    }








}
