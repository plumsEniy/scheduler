package com.bilibili.cluster.scheduler.api.service.bmr.spark;

import cn.hutool.core.net.url.UrlBuilder;
import cn.hutool.http.HttpRequest;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.bilibili.cluster.scheduler.common.Constants;
import com.bilibili.cluster.scheduler.common.dto.bmr.resourceV2.PageInfo;
import com.bilibili.cluster.scheduler.common.dto.bmr.experiment.ExperimentJobType;
import com.bilibili.cluster.scheduler.common.dto.bmr.experiment.ExperimentJobResultDTO;
import com.bilibili.cluster.scheduler.common.dto.spark.manager.SparkClientNodeInfo;
import com.bilibili.cluster.scheduler.common.dto.spark.manager.SparkJobInfoDTO;
import com.bilibili.cluster.scheduler.common.dto.spark.manager.SparkJobLabel;
import com.bilibili.cluster.scheduler.common.dto.spark.manager.SparkJobType;
import com.bilibili.cluster.scheduler.common.dto.spark.manager.SparkPeripheryComponentJobInfoDTO;
import com.bilibili.cluster.scheduler.common.dto.spark.manager.SparkTestJobDetailDTO;
import com.bilibili.cluster.scheduler.common.dto.spark.manager.req.QueryPeripheryComponentJobPageReq;
import com.bilibili.cluster.scheduler.common.dto.spark.manager.req.QuerySparkJobPageReq;
import com.bilibili.cluster.scheduler.common.dto.spark.manager.resp.QueryFallbackPackageResp;
import com.bilibili.cluster.scheduler.common.dto.spark.manager.resp.QueryPeripheryComponentJobPageResp;
import com.bilibili.cluster.scheduler.common.dto.spark.manager.resp.QueryRelationComponentAllJobListResp;
import com.bilibili.cluster.scheduler.common.dto.spark.manager.resp.QuerySparkJobPageResp;
import com.bilibili.cluster.scheduler.common.dto.spark.manager.resp.SparkClientAllNodeListResp;
import com.bilibili.cluster.scheduler.common.dto.spark.manager.resp.SparkJobDetailResp;
import com.bilibili.cluster.scheduler.common.dto.spark.manager.resp.SparkJobListResp;
import com.bilibili.cluster.scheduler.common.dto.spark.manager.resp.SparkTestJobDetailResp;
import com.bilibili.cluster.scheduler.common.dto.spark.periphery.SparkPeripheryComponent;
import com.bilibili.cluster.scheduler.common.dto.spark.periphery.req.SparkPeripheryComponentVersionInfoReq;
import com.bilibili.cluster.scheduler.common.dto.spark.periphery.req.SparkPeripheryComponentVersionUpdateReq;
import com.bilibili.cluster.scheduler.common.dto.spark.periphery.resp.SparkPeripheryComponentUpdateVersionInfoResp;
import com.bilibili.cluster.scheduler.common.dto.spark.periphery.resp.SparkPeripheryComponentVersionInfoDTO;
import com.bilibili.cluster.scheduler.common.dto.spark.periphery.resp.SparkPeripheryComponentVersionInfoResp;
import com.bilibili.cluster.scheduler.common.response.BaseMsgResp;
import com.bilibili.cluster.scheduler.common.utils.BaseRespUtil;
import com.bilibili.cluster.scheduler.common.utils.RetryUtils;
import com.google.common.base.Preconditions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

@Slf4j
@Component
public class SparkManagerServiceImpl implements SparkManagerService {

    @Value("${bmr.spark-manager.url}")
    String sparkManagerUrl;

    @Override
    public SparkJobInfoDTO querySparkJobInfo(String jobId, ExperimentJobType type, long testSetVersionId) {
        Callable<SparkJobInfoDTO> callable = () -> {
            String url;
            String resp;
            switch (type) {
                case COMPASS_JOB:
                    url = UrlBuilder.ofHttp(sparkManagerUrl).addPath("/spark/manager/spark/job/get/job/by/job/id")
                            .build();
                    JSONObject params = new JSONObject();
                    params.set("jobId", jobId);
                    resp = HttpRequest.post(url).header(Constants.BMR_SYSTEM_INVOKER_KEY, Constants.BMR_SYSTEM_TOKEN)
                            .body(JSONUtil.toJsonStr(params)).execute().body();
                    break;
                case TEST_JOB:
                    Preconditions.checkState(testSetVersionId > 0,
                            "testSetVersionId require > 0, but receive: " + testSetVersionId);
                    url = UrlBuilder.ofHttp(sparkManagerUrl).addPath("/spark/manager/spark/test/set/query/job/by/jobId")
                            .addQuery("testJobId", jobId)
                            .addQuery("testSetVersionId", String.valueOf(testSetVersionId))
                            .build();
                    resp = HttpRequest.get(url)
                            // .header("content-type", "application/x-www-form-urlencoded")
                            .header(Constants.BMR_SYSTEM_INVOKER_KEY, Constants.BMR_SYSTEM_TOKEN)
                            .execute().body();
                    break;
                default:
                    throw new IllegalArgumentException("unknown of ExperimentJobType: " + type);
            }
            log.info("querySparkJobInfo resp is {}", resp);

            switch (type) {
                case COMPASS_JOB:
                    SparkJobDetailResp jobDetailResp = JSONUtil.toBean(resp, SparkJobDetailResp.class);
                    BaseRespUtil.checkMsgResp(jobDetailResp);
                    return jobDetailResp.getObj();
                case TEST_JOB:
                    SparkTestJobDetailResp testJobDetailResp = JSONUtil.toBean(resp, SparkTestJobDetailResp.class);
                    BaseRespUtil.checkMsgResp(testJobDetailResp);
                    SparkTestJobDetailDTO testJobDetailDTO = testJobDetailResp.getObj();
                    return transferToJobDTO(testJobDetailDTO);
                default:
                    throw new IllegalArgumentException("unknown of ExperimentJobType: " + type);
            }
        };
        try {
            return RetryUtils.retryWith(3, 5, callable);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException("querySparkJobInfo error, jobId is " + jobId + ", type is " + type);
        }
    }

    private SparkJobInfoDTO transferToJobDTO(SparkTestJobDetailDTO testJobDetailDTO) {
        Preconditions.checkNotNull(testJobDetailDTO, "testJobDetailDTO is null");
        final SparkJobInfoDTO sparkJobInfoDTO = new SparkJobInfoDTO();
        sparkJobInfoDTO.setJobId(testJobDetailDTO.getJobId());
        sparkJobInfoDTO.setJobName(testJobDetailDTO.getName());
        sparkJobInfoDTO.setSqlStatement(testJobDetailDTO.getJobDetails());
        sparkJobInfoDTO.setJobType(SparkJobType.SQL);
        sparkJobInfoDTO.setJobOwner(testJobDetailDTO.getCreator());
        sparkJobInfoDTO.setDefaultRunningParameters(testJobDetailDTO.getDefaultRunningParameters());

        return sparkJobInfoDTO;
    }

    @Override
    public List<SparkJobInfoDTO> queryAllPublishJobExcludeByVersion(String minorVersion) {
        final String url = UrlBuilder.of(sparkManagerUrl)
                .addPath("/spark/manager/spark/job/query/all/publish/job/exclude/by/version")
                .addQuery("excludeVersion", minorVersion)
                .build();
        log.info("queryFullReleaseJobList url is {}", url);
        final String resp = HttpRequest.get(url)
                .header(Constants.BMR_SYSTEM_INVOKER_KEY, Constants.BMR_SYSTEM_TOKEN)
                .setReadTimeout(60_000)
                .execute().body();
        SparkJobListResp jobListResp = JSONUtil.toBean(resp, SparkJobListResp.class);
        BaseRespUtil.checkMsgResp(jobListResp);
        final List<SparkJobInfoDTO> jobInfoDTOList = jobListResp.getObj();

        if (CollectionUtils.isEmpty(jobInfoDTOList)) {
            return Collections.emptyList();
        } else {
            return jobInfoDTOList;
        }
    }

    @Override
    public boolean updateSparkVersion(String jobId, String targetSparkVersion) {
        final String url = UrlBuilder.ofHttp(sparkManagerUrl)
                .addPath("/spark/manager/spark/job/update/target/spark/version")
                .build();

        Map<String, String> params = new HashMap<>();
        params.put("jobId", jobId);
        params.put("targetSparkVersion", targetSparkVersion);
        String body = JSONUtil.toJsonStr(params);
        log.info("updateSparkVersion url is {}, request body is {}", url, body);
        try {
            final String resp = RetryUtils.retryWith(3, 5,
                    () -> HttpRequest.post(url)
                    .header(Constants.BMR_SYSTEM_INVOKER_KEY, Constants.BMR_SYSTEM_TOKEN)
                    .body(body)
                    .execute().body());
            log.info("updateSparkVersion resp is {}", resp);
            BaseMsgResp baseMsgResp = JSONUtil.toBean(resp, BaseMsgResp.class);
            BaseRespUtil.checkMsgResp(baseMsgResp);
            return true;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean lockSparkJobVersion(String jobId, boolean lockOrNot) {
        final String url = UrlBuilder.ofHttp(sparkManagerUrl)
                .addPath("/spark/manager/spark/job/batch/lock/spark/version")
                .build();
        Map<String, Object> params = new HashMap<>();
        params.put("jobIds", Arrays.asList(jobId));
        params.put("lockSparkVersion", lockOrNot);
        String body = JSONUtil.toJsonStr(params);
        log.info("lockSparkJobVersion url is {}, request body is {}", url, body);
        try {
            final String resp = RetryUtils.retryWith(3, 5,
                    () -> HttpRequest.post(url)
                            .header(Constants.BMR_SYSTEM_INVOKER_KEY, Constants.BMR_SYSTEM_TOKEN)
                            .body(body)
                            .execute().body());
            log.info("lockSparkJobVersion resp is {}", resp);

            BaseMsgResp baseMsgResp = JSONUtil.toBean(resp, BaseMsgResp.class);
            BaseRespUtil.checkMsgResp(baseMsgResp);
            return true;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return false;
        }
    }

    @Override
    public List<String> queryTargetVersionJobList(String originalSparkVersion) {
        List<String> jobList = new ArrayList<>();
        boolean hasNextPage = true;
        int indexPage = 1;
        int defaultPageSize = 1000;
        while (hasNextPage) {
            List<SparkJobInfoDTO> currentPageData = queryTargetVersionJobDtoList(originalSparkVersion, indexPage++, defaultPageSize);
            if (CollectionUtils.isEmpty(currentPageData)) {
                hasNextPage = false;
                break;
            }
            currentPageData.stream().forEach(e -> jobList.add(e.getJobId()));
        }
        return jobList;
    }

    @Override
    public boolean updateSparkCiJobInfo(ExperimentJobResultDTO jobResultDTO) {
        try {
            String path = "spark/manager/spark/ci/test/runInfoUpdate";
            final String url = UrlBuilder.ofHttp(sparkManagerUrl).addPath(path).build();
            String body = JSONUtil.toJsonStr(jobResultDTO);

            log.info("updateSparkCiJobInfo url is {}, request body is {}", url, body);
            final String resp = HttpRequest.post(url)
                    .header(Constants.BMR_SYSTEM_INVOKER_KEY, Constants.BMR_SYSTEM_TOKEN)
                    .body(body)
                    .execute().body();
            log.info("updateSparkCiJobInfo resp is {}", resp);
            BaseMsgResp baseMsgResp = JSONUtil.toBean(resp, BaseMsgResp.class);
            BaseRespUtil.checkMsgResp(baseMsgResp);
            return true;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return false;
        }
    }

    private List<SparkJobInfoDTO> queryTargetVersionJobDtoList(String originalSparkVersion, int pageName, int pageSize) {
        String path = "/spark/manager/spark/job/get/page/list";
        final String url = UrlBuilder.ofHttp(sparkManagerUrl).addPath(path).build();

        final QuerySparkJobPageReq jobPageReq = new QuerySparkJobPageReq();
        jobPageReq.setTargetSparkVersion(originalSparkVersion);
        jobPageReq.setPageNum(pageName);
        jobPageReq.setPageSize(pageSize);
        String body = JSONUtil.toJsonStr(jobPageReq);
        log.info("queryTargetVersionJobList url is {}, request body is {}", url, body);
        final String resp = HttpRequest.post(url)
                .header(Constants.BMR_SYSTEM_INVOKER_KEY, Constants.BMR_SYSTEM_TOKEN)
                .setReadTimeout(60_000)
                .body(body)
                .execute().body();
        QuerySparkJobPageResp jobPageResp = JSONUtil.toBean(resp, QuerySparkJobPageResp.class);

        BaseRespUtil.checkMsgResp(jobPageResp);
        PageInfo<SparkJobInfoDTO> pageInfo = jobPageResp.getObj();
        final List<SparkJobInfoDTO> jobInfoDTOList = pageInfo.getList();

        return jobInfoDTOList;
    }

    @Override
    public List<String> querySparkClientAllNodes() {
        String path = "/spark/manager/one/client/query/all/host";
        final String url = UrlBuilder.ofHttp(sparkManagerUrl).addPath(path).build();
        log.info("querySparkClientAllNodes url is {}", url);
        final String resp = HttpRequest.post(url)
                .header(Constants.BMR_SYSTEM_INVOKER_KEY, Constants.BMR_SYSTEM_TOKEN)
                .setReadTimeout(60_000)
                .execute().body();

        SparkClientAllNodeListResp allNodeListResp = JSONUtil.toBean(resp, SparkClientAllNodeListResp.class);
        BaseRespUtil.checkMsgResp(allNodeListResp);

        final List<SparkClientNodeInfo> nodeInfoList = allNodeListResp.getObj();
        if (CollectionUtils.isEmpty(nodeInfoList)) {
            return Collections.emptyList();
        }
        return nodeInfoList.stream().map(SparkClientNodeInfo::getHostname).collect(Collectors.toList());
    }

    @Override
    public String queryCurrentSparkDefaultVersion() {
        UrlBuilder urlBuilder = UrlBuilder.of(sparkManagerUrl)
                .addPath("/bmr-cluster-metadata/app/api/bmr/installationPackage/query/spark/default/package");
        String url = urlBuilder.build();
        log.info("query spark default package url is {}", url);
        String respStr = HttpRequest.get(url)
                .header(Constants.BMR_SYSTEM_INVOKER_KEY, Constants.BMR_SYSTEM_TOKEN)
                .execute().body();
        log.info("query spark default package, resp is {}", respStr);
        Preconditions.checkState(StringUtils.hasText(respStr), "服务端异常，resp为空");
        QueryFallbackPackageResp resp = JSONUtil.toBean(respStr, QueryFallbackPackageResp.class);
        if (resp.getCode().equals(1000)) {
            // 当前不存在默认的版本信息
            return Constants.EMPTY_STRING;
        }
        BaseRespUtil.checkMsgResp(resp);
        return resp.getObj().getMinorVersion();
    }

    @Override
    public List<String> queryAllReleaseJobList(SparkPeripheryComponent component, String excludeTargetVersion) {
        String path = "/spark/manager/spark/job/query/periphery/component/all/job/List";
        String url = UrlBuilder.of(sparkManagerUrl).addPath(path)
                .addQuery("component", component.name())
                .addQuery("excludeTargetVersion", excludeTargetVersion)
                .build();
        log.info("query queryAllReleaseJobList url is {}", url);
        String respStr = HttpRequest.get(url)
                .header(Constants.BMR_SYSTEM_INVOKER_KEY, Constants.BMR_SYSTEM_TOKEN)
                .timeout(30_000)
                .execute().body();

        QueryRelationComponentAllJobListResp resp = JSONUtil.toBean(respStr, QueryRelationComponentAllJobListResp.class);
        BaseRespUtil.checkMsgResp(resp);
        return resp.getObj();
    }

    @Override
    public Map<String, Set<String>> queryAllReleaseStageWithJobs(SparkPeripheryComponent component, String excludeTargetVersion) {
        String path = "/spark/manager/spark/job/common/query/spark/component/page/job/list";
        String url = UrlBuilder.of(sparkManagerUrl).addPath(path).build();
        final QueryPeripheryComponentJobPageReq queryReq = new QueryPeripheryComponentJobPageReq();
        queryReq.setComponent(component);
        queryReq.setExcludedTargetVersion(excludeTargetVersion);

        String body = JSONUtil.toJsonStr(queryReq);
        log.info("query queryAllReleaseStageWithJobs url is {}, body is {}", url, body);
        final String resp = HttpRequest.post(url)
                .header(Constants.BMR_SYSTEM_INVOKER_KEY, Constants.BMR_SYSTEM_TOKEN)
                .setReadTimeout(100_000)
                .body(body)
                .execute().body();

        QueryPeripheryComponentJobPageResp jobPageResp = JSONUtil.toBean(resp, QueryPeripheryComponentJobPageResp.class);
        BaseRespUtil.checkMsgResp(jobPageResp);
        final Map<String, Set<String>> resultMap = new LinkedHashMap<>();

        PageInfo<SparkPeripheryComponentJobInfoDTO> pageInfo = jobPageResp.getObj();
        List<SparkPeripheryComponentJobInfoDTO> jobInfoDTOList = pageInfo.getList();
        if (CollectionUtils.isEmpty(jobInfoDTOList)) {
            return resultMap;
        }

        Map<String, List<SparkPeripheryComponentJobInfoDTO>> stagedJobs = jobInfoDTOList.stream()
                .collect(Collectors.groupingBy(SparkPeripheryComponentJobInfoDTO::getLabel));

        int stage = 1;
        for (SparkJobLabel sparkJobLabel : SparkJobLabel.getStageList()) {
            final String label = sparkJobLabel.name();
            final List<SparkPeripheryComponentJobInfoDTO> jobInfoDTOS = stagedJobs.getOrDefault(label, Collections.EMPTY_LIST);
            if (CollectionUtils.isEmpty(jobInfoDTOS)) {
                continue;
            }
            resultMap.put(String.valueOf(stage), jobInfoDTOS.stream().map(SparkPeripheryComponentJobInfoDTO::getJobId)
                    .collect(Collectors.toCollection(LinkedHashSet::new)));
            stage++;
        }
        return resultMap;
    }

    @Override
    public SparkPeripheryComponentVersionInfoDTO querySparkPeripheryComponentVersionInfo(SparkPeripheryComponentVersionInfoReq req) {
        return RetryUtils.retryFunction(() -> {
            String path = "/spark/manager/spark/job/query/periphery/component/one/job/info";
            String url = UrlBuilder.of(sparkManagerUrl).addPath(path).build();
            String body = JSONUtil.toJsonStr(req);
            log.info("querySparkPeripheryComponentVersionInfo url is {}, body is {}", url, body);
            final String respStr = HttpRequest.post(url)
                    .header(Constants.BMR_SYSTEM_INVOKER_KEY, Constants.BMR_SYSTEM_TOKEN)
                    .body(body)
                    .timeout(10_000)
                    .execute().body();
            SparkPeripheryComponentVersionInfoResp resp = JSONUtil.toBean(respStr, SparkPeripheryComponentVersionInfoResp.class);
            BaseRespUtil.checkMsgResp(resp);
            return resp.getObj();
        });
    }

    @Override
    public boolean updateSparkPeripheryComponentVersion(SparkPeripheryComponentVersionUpdateReq versionUpdateReq) {
        return RetryUtils.retryFunction(() -> {
            String path = "/spark/manager/spark/job/update/periphery/component/version/info";
            String url = UrlBuilder.of(sparkManagerUrl).addPath(path).build();
            String body = JSONUtil.toJsonStr(versionUpdateReq);
            log.info("querySparkPeripheryComponentVersionInfo url is {}, body is {}", url, body);
            final String respStr = HttpRequest.post(url)
                    .header(Constants.BMR_SYSTEM_INVOKER_KEY, Constants.BMR_SYSTEM_TOKEN)
                    .body(body)
                    .timeout(10_000)
                    .execute().body();
            SparkPeripheryComponentUpdateVersionInfoResp resp = JSONUtil.toBean(respStr, SparkPeripheryComponentUpdateVersionInfoResp.class);
            BaseRespUtil.checkMsgResp(resp);
            return resp.getObj();
        });
    }

}
