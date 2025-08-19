package com.bilibili.cluster.scheduler.api.service.scheduler.pipeline;

import cn.hutool.core.lang.UUID;
import cn.hutool.core.net.url.UrlBuilder;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.json.JSONUtil;
import com.bilibili.cluster.scheduler.api.event.factory.FactoryDiscoveryUtils;
import com.bilibili.cluster.scheduler.api.event.factory.PipelineFactory;
import com.bilibili.cluster.scheduler.common.Constants;
import com.bilibili.cluster.scheduler.common.dto.bmr.config.resp.QueryConfigGroupInfoByIdResp;
import com.bilibili.cluster.scheduler.common.utils.BaseRespUtil;
import org.junit.Test;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;

public class PipelineFactoryTestUnit {


    @Test
    public void testFactory() {
        PipelineFactory factory = FactoryDiscoveryUtils.getFactoryByIdentifier("Amiya", PipelineFactory.class);
        System.out.println(factory);
    }

    @Test
    public void testRandom() {
        int total = 1000000000, i =0,  match = 0;
//        while (i>0) {
//            long instanceId = RandomUtil.randomLong(12);
//            System.out.println(instanceId);
//            i--;
//        }

        while (!(i ++> total)) {
            if (i % 2 == 0 || i % 3 == 0) {
                match++;
            }
        }

        System.out.println((match * 1.0 + 1) / i);
    }

    @Test
    public void testConfigId() {
        long configId = 1702;
        String BASE_URL = "http://pre-cloud-bm.bilibili.co";
        String url = UrlBuilder.of(BASE_URL)
                .addPath("/bmr/config/service/api/config/version/query-version-group-and-file")
                // .addQuery("versionId", "1702")
                .build();
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("versionId", 1702);
        String respStr = HttpRequest.get(url)
                .form(map)
                 // .body(JSONUtil.toJsonStr(map))
                .header(Constants.BMR_SYSTEM_INVOKER_KEY, Constants.BMR_SYSTEM_TOKEN)
                .execute().body();

//        String url = UrlBuilder.of(BASE_URL)
//                .addPath("/bmr/config/service/api/config/version/query-version-group-and-file")
//                .addQuery("versionId", String.valueOf(configId))
//                .build();
//        String respStr = HttpRequest.get(url)
//                .header(Constants.BMR_SYSTEM_INVOKER_KEY, Constants.BMR_SYSTEM_TOKEN)
//                .execute().body();
        System.out.println(respStr);
        QueryConfigGroupInfoByIdResp resp = JSONUtil.toBean(respStr, QueryConfigGroupInfoByIdResp.class);
        BaseRespUtil.checkMsgResp(resp);
        System.out.println(resp.getObj().getLogicGroups());
    }

    @Test
    public void testPercent() {

        int totalCnt = 10, successCnt = 10;

        DecimalFormat df = new DecimalFormat("#0.00");
        String successPercent = df.format(successCnt * 100.0 / totalCnt);
        String unSuccessPercent = df.format((totalCnt - successCnt) * 100.0 / totalCnt);

        System.out.println(successPercent);
        System.out.println(unSuccessPercent);

    }

    @Test
    public void testPercent2() {
        Double memoryCostMB = 0.0D;
        System.out.println(formatMemoryToGB(memoryCostMB, 0.0D));
    }


    private Double formatMemoryToGB(Double memoryCost, Double defaultValue) {
        if (memoryCost.doubleValue() > defaultValue.doubleValue()) {
            DecimalFormat df = new DecimalFormat("#0.00");
            String memoryGB = df.format(memoryCost / 1024.0);
            return Double.parseDouble(memoryGB);
        } else {
            return defaultValue;
        }
    }

    @Test
    public void testGeneUuid() {
        String id = UUID.fastUUID().toString().replace(Constants.BAR, Constants.EMPTY_STRING);

        id = java.util.UUID.randomUUID().toString().replace(Constants.BAR, Constants.EMPTY_STRING);
        System.out.println(id);

        double gapPercent = 200.13409944894;
        gapPercent = 0.0569;
        final DecimalFormat df = new DecimalFormat("#0.00");
        final String gapPercentFmt = df.format(gapPercent);

        System.out.println(gapPercentFmt);

    }

}
