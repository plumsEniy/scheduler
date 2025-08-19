package com.bilibili.cluster.scheduler.api.service.bmr.spark.ess;

import cn.hutool.http.Header;
import cn.hutool.http.HttpRequest;
import cn.hutool.json.JSONUtil;
import com.bilibili.cluster.scheduler.common.Constants;
import com.bilibili.cluster.scheduler.common.dto.spark.ess.BlackList;
import com.bilibili.cluster.scheduler.common.dto.spark.ess.resp.BlackListResp;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class SparkEssMasterServiceImpl implements SparkEssMasterService {

    @Override
    public boolean addBlackList(List<String> hostList, String sparkEssMasterHostName) {
        if (CollectionUtils.isEmpty(hostList)) {
            log.warn("addBlackList主机列表不能为空...");
            return false;
        }

        BlackList blackList = new BlackList();
        blackList.setHosts(hostList);
        List<String> types = new ArrayList<>();
        types.add("add");
        blackList.setType(types);
        return requestSparkEssMasterBlack(blackList, sparkEssMasterHostName);
    }

    @Override
    public boolean removeBlackList(List<String> hostList, String sparkEssMasterHostName) {
        if (CollectionUtils.isEmpty(hostList)) {
            log.warn("addBlackList主机列表不能为空...");
            return false;
        }

        BlackList blackList = new BlackList();
        blackList.setHosts(hostList);
        List<String> types = new ArrayList<>();
        types.add("remove");
        blackList.setType(types);
        return requestSparkEssMasterBlack(blackList, sparkEssMasterHostName);
    }

    private boolean requestSparkEssMasterBlack(BlackList blackList, String sparkEssMasterHostName) {
        String hostName;
        if (!sparkEssMasterHostName.contains(Constants.HOST_SUFFIX)) {
            hostName = sparkEssMasterHostName.concat(Constants.HOST_SUFFIX);
        } else {
            hostName = sparkEssMasterHostName;
        }

        String requestUrl = "http://" + hostName + ":10068/blacklist";
        String res = HttpRequest.post(requestUrl)
                .header(Header.CONTENT_TYPE, "application/json")
                .body(JSONUtil.toJsonStr(blackList))
                .timeout(20000)// 超时，毫秒
                .execute().body();
        log.info("request url: {}, res: {}", requestUrl, res);
        if (!StringUtils.hasText(res)) {
            return false;
        }

        BlackListResp blackListRes = JSONUtil.toBean(res, BlackListResp.class);
        if (blackListRes.getCode() == 0) {
            return true;
        }
        return false;
    }

}
