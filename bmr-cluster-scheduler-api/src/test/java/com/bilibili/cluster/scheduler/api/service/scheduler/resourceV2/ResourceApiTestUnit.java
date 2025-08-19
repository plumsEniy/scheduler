package com.bilibili.cluster.scheduler.api.service.scheduler.resourceV2;

import com.bilibili.cluster.scheduler.api.service.bmr.resourceV2.BmrResourceV2ServiceImpl;
import com.bilibili.cluster.scheduler.common.dto.bmr.resourceV2.model.ResourceHostInfo;
import org.junit.Test;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.List;

public class ResourceApiTestUnit {

    @Test
    public void test() {

        final BmrResourceV2ServiceImpl apiService = new BmrResourceV2ServiceImpl();

        final List<String> hostList = Arrays.asList(
//                "jscs-bigdata-test-31",
//                "jscs-bigdata-test-32",
//                "jscs-bigdata-test-33",
                "jscs-bigdata-test-34"
        );


        final List<ResourceHostInfo> hostInfoList = apiService.queryHostInfoByName(hostList);

        for (ResourceHostInfo resourceHostInfo : hostInfoList) {
            System.out.println(resourceHostInfo);
        }



    }



}
