package com.bilibili.cluster.scheduler.api.service.bmr.yarn;

import cn.hutool.http.HttpRequest;
import cn.hutool.json.JSONUtil;
import com.bilibili.cluster.scheduler.common.Constants;
import com.bilibili.cluster.scheduler.common.dto.yarn.req.AmiyaGracefullyOffReq;
import com.bilibili.cluster.scheduler.common.dto.yarn.req.AmiyaOffInitResourceReq;
import com.bilibili.cluster.scheduler.common.dto.yarn.resp.QueryNodeManagerContainerResp;
import com.bilibili.cluster.scheduler.common.exception.RequesterException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class YarnNodeManagerServiceImpl implements YarnNodeManagerService {

    @Override
    public Map<String, Boolean> checkContainers(List<String> hostList) {
        //若所有NM节点都ready下线了，则total为true，否则为false
        Map<String, Boolean> resMap = new HashMap<>();
        for (String hostName : hostList) {
            checkEachHostContainer(hostName, resMap);
        }
        if (!resMap.containsKey("total")) {
            resMap.put("total", true);
        }
        return resMap;
    }

    @Override
    public void amiyaOff(List<String> hostList) {
        if (CollectionUtils.isEmpty(hostList)) {
            return;
        }
        for (String hostName : hostList) {
            amiyaOffPerHost(hostName);
        }
    }

    private void amiyaOffPerHost(String hostName) {
        String baseUri = String.format("%s%s%s", Constants.HTTP_PROTOCOL, hostName, Constants.BILIBILI_HOST_SUFFIX);
        try {
            String amiyaStopUrl = baseUri + ":10030/stop";
            final String resp = HttpRequest.get(amiyaStopUrl).timeout(6_000).execute().body();
            log.info("Amiya has finished stop step1, http resp is: {}.", resp);
        } catch (Exception e) {
            log.error("ignore amiya process stop.");
        }
        for (int i = 0; i < 3; i++) {
            try {
                String amiyaInitResourceUrl = baseUri + ":8042/ws/v1/node/initResource";
                final String resp = HttpRequest.post(amiyaInitResourceUrl).timeout(6_000).execute().body();
                log.info("Amiya has finished stop step2, http resp is" + resp);
                return;
            } catch (Exception e) {
                log.error("Amiya stop step2 error {}", e.getMessage());
                try {
                    Thread.sleep(500);
                } catch (InterruptedException ex) {
                   // ignore
                    return;
                }
            }
        }
    }

    @Override
    public void amiyaGracefullyOff(List<String> hostList, int stopWaitTimeSecond, int stopWaitLogUploadTimeSecond) {
        if (CollectionUtils.isEmpty(hostList)) {
            return;
        }
        for (String hostName : hostList) {
            amiyaGracefullyOffPerHost(hostName, stopWaitTimeSecond, stopWaitLogUploadTimeSecond);
        }
    }

    private void amiyaGracefullyOffPerHost(String hostName, int stopWaitTimeSecond, int stopWaitLogUploadTimeSecond) {
        String baseUri = String.format("%s%s%s", Constants.HTTP_PROTOCOL, hostName, Constants.BILIBILI_HOST_SUFFIX);;
        AmiyaGracefullyOffReq req = new AmiyaGracefullyOffReq("gracefully", stopWaitTimeSecond, stopWaitLogUploadTimeSecond);
        try {
            String amiyaGracefullOffUrl = baseUri + ":10030/stopNM";
            final String resp = HttpRequest.post(amiyaGracefullOffUrl).timeout(10_000).body(JSONUtil.toJsonStr(req)).execute().body();
            log.info("Amiya has finished stop step1, http url:{}, resp is {}", baseUri, resp);
        } catch (Exception e) {
            log.error("ignore amiya gracefuuly stop.", e);
        }
    }

    @Override
    public void updateResourceZero(List<String> hostList) {
        if (CollectionUtils.isEmpty(hostList)) {
            return;
        }
        for (String hostName : hostList) {
            updateResourceZeroEachHost(hostName);
        }
    }

    private void updateResourceZeroEachHost(String hostName) {
        String baseUri = String.format("%s%s%s", "http://", hostName, Constants.BILIBILI_HOST_SUFFIX);;
        for (int i = 0; i < 3; i++) {
            try {
                AmiyaOffInitResourceReq req = new AmiyaOffInitResourceReq();
                String updateResourceZeroUrl = baseUri + ":8042/ws/v1/node/resourceUpdate";
                final String resp = HttpRequest.post(updateResourceZeroUrl).timeout(10_000)
                        .body(JSONUtil.toJsonStr(req)).execute().body();
                log.info("invoker nm update Resource to Zero, resp is {}.", resp);
                return;
            } catch (Exception e) {
                log.error("fail to invoker nm update Resource to Zero : {}", e.getMessage());
                try {
                    Thread.sleep(500);
                } catch (InterruptedException ex) {
                    // ignore
                    return;
                }
            }
        }
    }

    public void checkEachHostContainer(String hostName, Map<String, Boolean> resMap) {
        //检测该host上的container是否都运行完了
        String baseUri = String.format("%s%s%s", Constants.HTTP_PROTOCOL, hostName, Constants.BILIBILI_HOST_SUFFIX);
        String url = baseUri + ":8042/ws/v1/node/containers";

        for (int i = 0; i < 3; i++) {
            try {
                final String respStr = HttpRequest.get(url).timeout(10_000).execute().body();
                QueryNodeManagerContainerResp resp = JSONUtil.toBean(respStr, QueryNodeManagerContainerResp.class);
                log.info("invoker nm rest api containers resp is {}", resp);
                if (resp.getContainers() != null) {
                    resMap.put(hostName, false);
                    resMap.put("total", false);
                    log.info(hostName + "'s container is not empty");
                } else {
                    resMap.put(hostName, true);
                }
                return;
            } catch (RequesterException ex) {
                log.warn("request http host error", ex);
                try {
                    Thread.sleep(500);
                } catch (InterruptedException runtimeEx) {
                    throw new RuntimeException(runtimeEx);
                }
            }
        }
        resMap.put(hostName, true);
    }
}
