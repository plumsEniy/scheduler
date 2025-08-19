package com.bilibili.cluster.scheduler.api.service.bmr.yarn;


import java.util.List;
import java.util.Map;

public interface YarnNodeManagerService {

    Map<String, Boolean> checkContainers(List<String> hostList);

    void amiyaOff(List<String> hostList);

    void amiyaGracefullyOff(List<String> hostList, int stopWaitTimeSecond, int stopWaitLogUploadTimeSecond);

    void updateResourceZero(List<String> hostList);

}
