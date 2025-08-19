package com.bilibili.cluster.scheduler.api.service.scheduler;

import cn.hutool.core.net.url.UrlBuilder;
import cn.hutool.http.Header;
import cn.hutool.http.HttpRequest;
import cn.hutool.json.JSONUtil;
import com.bilibili.cluster.scheduler.api.exceptions.DolphinSchedulerInvokerException;
import com.bilibili.cluster.scheduler.common.Constants;
import com.bilibili.cluster.scheduler.common.dolphin.DolphinPipelineDefinition;
import com.bilibili.cluster.scheduler.common.dolphin.DolphinPipelineResolveParameter;
import com.bilibili.cluster.scheduler.common.dto.scheduler.ExecutionInstanceDetail;
import com.bilibili.cluster.scheduler.common.dto.scheduler.model.DagData;
import com.bilibili.cluster.scheduler.common.dto.scheduler.model.PipelineDefine;
import com.bilibili.cluster.scheduler.common.dto.scheduler.model.ProcessTaskRelation;
import com.bilibili.cluster.scheduler.common.dto.scheduler.model.ProjectDefine;
import com.bilibili.cluster.scheduler.common.dto.scheduler.model.SchedInstanceData;
import com.bilibili.cluster.scheduler.common.dto.scheduler.model.SchedTaskDefine;
import com.bilibili.cluster.scheduler.common.dto.scheduler.model.TaskInstance;
import com.bilibili.cluster.scheduler.common.dto.scheduler.req.OperateSchedInstanceReq;
import com.bilibili.cluster.scheduler.common.dto.scheduler.req.StartPipelineReq;
import com.bilibili.cluster.scheduler.common.dto.scheduler.resp.*;
import com.bilibili.cluster.scheduler.common.enums.scheduler.DolpFailureStrategy;
import com.bilibili.cluster.scheduler.common.enums.scheduler.ExecuteType;
import com.bilibili.cluster.scheduler.common.enums.scheduler.WorkflowExecutionStatus;
import com.bilibili.cluster.scheduler.common.exception.RequesterException;
import com.bilibili.cluster.scheduler.common.utils.BaseRespUtil;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * @description:
 * @Date: 2024/5/13 17:32
 * @Author: nizhiqiang
 */
@Service
@Slf4j
public class DolphinSchedulerInteractServiceImpl implements DolphinSchedulerInteractService {

    @Value("${dolphin.base-url}")
    private String BASE_URL;

//    @Value("${dolphin.token}")
//    private String token;
//
//    @Value("${dolphin.dqc.token}")
//    private String dqcToken;
//
    @Value("${dolphin.admin.token}")
    private String adminToken;

    @Override
    public PipelineDefine queryPipelineDefine(String projectName, String pipelineName) throws DolphinSchedulerInvokerException {
        try {
            log.info("dolphin-scheduler queryPipelineDefine by projectName={}, pipelineNam={}", projectName, pipelineName);
            PipelineDefine pipelineDefine = new PipelineDefine();
            Optional<ProjectDefine> projectDefineOptional = queryProjectByName(projectName);
            ProjectDefine projectDefine = projectDefineOptional.get();
            String projectCode = projectDefine.getCode();
            DagData dagData = getDagDataByName(projectCode, pipelineName);
            LinkedList<SchedTaskDefine> schedTaskDefineList = sortTaskDefines(dagData);
            pipelineDefine.setProjectCode(projectCode);
            pipelineDefine.setProjectName(projectName);
            String code = dagData.getProcessDefinition().getCode();
            String name = dagData.getProcessDefinition().getName();
            pipelineDefine.setPipelineCode(code);
            pipelineDefine.setPipelineName(name);
            pipelineDefine.setSchedTaskDefineList(schedTaskDefineList);
            return pipelineDefine;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new DolphinSchedulerInvokerException(e.getMessage(), e);
        }
    }

    @Override
    public List<SchedTaskDefine> parsePipelineDefineByCode(String projectCode, String pipelineCode) {
        DagData dagData = getDagDataBycode(projectCode, pipelineCode);
        LinkedList<SchedTaskDefine> schedTaskDefineList = sortTaskDefines(dagData);
        return schedTaskDefineList;
    }

    private LinkedList<SchedTaskDefine> sortTaskDefines(DagData dagData) {

        //map的key为该taskDefine的前置节点的TaskCode，value为该taskDefine
        HashMap<String, SchedTaskDefine> map = new HashMap<>();

        LinkedList<SchedTaskDefine> resList = new LinkedList<>();
        for (ProcessTaskRelation ptrl : dagData.getProcessTaskRelationList()) {
            map.put(ptrl.getPreTaskCode(), new SchedTaskDefine(dagData, ptrl));
        }
        String currentCode = "0";
        for (int i = 0; i < map.size(); i++) {
            resList.add(map.get(currentCode));
            currentCode = map.get(currentCode).getTaskCode();
        }
        return resList;
    }

    @Override
    public String startPipeline(String projectCode, String pipelineCode, Map<String, Object> execEnv, DolpFailureStrategy failureStrategy) {
        String url = UrlBuilder.ofHttp(BASE_URL)
                .addPath(String.format("/projects/%s/executors/start-process-instance", projectCode))
                .build();

        StartPipelineReq req = new StartPipelineReq(pipelineCode, execEnv, failureStrategy);
        String reqStr = JSONUtil.toJsonStr(req);
        log.info("start pipe line, project id is {}, req is {}", projectCode, reqStr);
        String respStr = HttpRequest
                .post(url)
                .header(Constants.TOKEN, MDC.get(Constants.DOLPHIN_SCHEDULER_TOKEN_KEY))
                .header(Constants.CONTENT_TYPE, Constants.FOMR_URLENCODED)
                .form("processDefinitionCode", pipelineCode)
                .form("failureStrategy", failureStrategy)
                .form("processInstancePriority", "MEDIUM")
                .form("scheduleTime", "")
                .form("warningType", "NONE")
                .form("startParams", JSONUtil.toJsonStr(execEnv))
                .execute().body();
        StartPipelineResp resp = JSONUtil.toBean(respStr, StartPipelineResp.class);
        BaseRespUtil.checkDolphinSchedulerResp(resp);
        return resp.getData();
    }

    @Override
    public String querySchedInstanceStatus(String projectCode, String schedInstanceId) {
        SchedInstanceResp resp = querySchedInstance(projectCode, schedInstanceId);
        if (resp.getData() != null && resp.getData().getState() != null) {
            return resp.getData().getState();
        } else {
            return null;
        }
    }

    @Override
    public ExecutionInstanceDetail querySchedInstanceTaskDetail(String projectCode, String schedInstanceId) {
        SchedInstanceResp resp = querySchedInstance(projectCode, schedInstanceId);
        return new ExecutionInstanceDetail(resp);
    }

    /**
     * 查询实例
     *
     * @param projectCode
     * @param schedInstanceId
     * @return
     */
    public SchedInstanceResp querySchedInstance(String projectCode, String schedInstanceId) {
        String url = UrlBuilder.ofHttp(BASE_URL)
                .addPath(String.format("/projects/%s/process-instances/execute/%s", projectCode, schedInstanceId))
                .build();

        log.info("query instance url is {}", url);
        String respStr = HttpRequest.get(url)
                .header(Constants.TOKEN, MDC.get(Constants.DOLPHIN_SCHEDULER_TOKEN_KEY))
                .execute().body();
        SchedInstanceResp resp = JSONUtil.toBean(respStr, SchedInstanceResp.class);
        log.info("query instance resp is {}", resp);
        BaseRespUtil.checkDolphinSchedulerResp(resp);
        return resp;
    }

    @Override
    public boolean pauseSchedInstance(String projectCode, String schedInstanceId) {
        ExecutionInstanceDetail edl = querySchedInstanceTaskDetail(projectCode, schedInstanceId);
        if (!edl.isAlreadyExec()) return false;
        if (WorkflowExecutionStatus.isPause(edl.getState())) return true;
        if (WorkflowExecutionStatus.isFinish(edl.getState())) return true;

        OperateSchedInstanceResp resp = operateSchedInstance(projectCode, edl, ExecuteType.PAUSE);
        return resp.isSuccess();
    }

    /**
     * 操作状态
     *
     * @param projectCode
     * @param edl
     * @return
     */
    private OperateSchedInstanceResp operateSchedInstance(String projectCode, ExecutionInstanceDetail edl, ExecuteType executeType) {
        String url = UrlBuilder.ofHttp(BASE_URL).addPath(String.format("/projects/%s/executors/execute", projectCode)).build();
        int processInstanceId = edl.getProcessInstanceId();
        OperateSchedInstanceReq req = new OperateSchedInstanceReq(processInstanceId, executeType);
        String respStr = HttpRequest
                .post(url).header(Constants.TOKEN, MDC.get(Constants.DOLPHIN_SCHEDULER_TOKEN_KEY))
                .form("processInstanceId", processInstanceId)
                .form("executeType", executeType)
                .execute().body();
        OperateSchedInstanceResp resp = JSONUtil.toBean(respStr, OperateSchedInstanceResp.class);
        BaseRespUtil.checkDolphinSchedulerResp(resp);
        return resp;
    }

    @Override
    public boolean resumeSchedInstance(String projectCode, String schedInstanceId) throws InterruptedException {
        ExecutionInstanceDetail edl = querySchedInstanceTaskDetail(projectCode, schedInstanceId);
        if (!edl.isAlreadyExec()) return false;
        if (WorkflowExecutionStatus.isFinish(edl.getState())) return true;
        if (WorkflowExecutionStatus.isRunning(edl.getState())) return true;
        if (!WorkflowExecutionStatus.isPause(edl.getState())) {
            throw new RuntimeException(String.format("[schedInstanceId %s]'s status can not resume, so fail to resume, its status is [%s]", schedInstanceId, edl.getState()));
        }

        OperateSchedInstanceResp resp = operateSchedInstance(projectCode, edl, ExecuteType.RECOVER_SUSPENDED_PROCESS);
        return resp.isSuccess();
    }

    @Override
    public boolean retryFailureTask(String projectCode, String schedInstanceId) {
        ExecutionInstanceDetail edl = querySchedInstanceTaskDetail(projectCode, schedInstanceId);
        if (WorkflowExecutionStatus.isRunning(edl.getState())) return true;
        if (!WorkflowExecutionStatus.isFailure(edl.getState())) {
            throw new RuntimeException(String.format("[schedInstanceId %s]'s status can not start failure task, so fail to recovery, its status is [%s]", schedInstanceId, edl.getState()));
        }
        ;

        OperateSchedInstanceResp resp = operateSchedInstance(projectCode, edl, ExecuteType.START_FAILURE_TASK_PROCESS);
        return resp.isSuccess();
    }

    @Override
    public DolphinPipelineDefinition resolvePipelineDefinition(DolphinPipelineResolveParameter dolphinPipelineResolveParameter) throws DolphinSchedulerInvokerException {
        String projectName = new StringBuilder(dolphinPipelineResolveParameter.getRoleName())
                .append(Constants.UNDER_LINE)
                .append(dolphinPipelineResolveParameter.getClusterAlias()).toString();
        StringBuilder pipelineNameBuilder = new StringBuilder(dolphinPipelineResolveParameter.getComponentName())
                .append(Constants.UNDER_LINE)
                .append(dolphinPipelineResolveParameter.getFlowDeployType().getDolphinAlias());
        int chainedIndex = dolphinPipelineResolveParameter.getChainedIndex();

        if (chainedIndex > 0) {
            pipelineNameBuilder.append(Constants.UNDER_LINE).append(chainedIndex);
        }
        String pipelineName = pipelineNameBuilder.toString();
        PipelineDefine pipelineDefine = queryPipelineDefine(projectName, pipelineName);
        log.info("queryPipelineDefine result is {}", pipelineDefine);
        DolphinPipelineDefinition pipelineDefinition = new DolphinPipelineDefinition();
        pipelineDefinition.setProjectCode(pipelineDefine.getProjectCode());
        pipelineDefinition.setPipelineCode(pipelineDefine.getPipelineCode());
        pipelineDefinition.setSchedulerPipelineChainIndex(dolphinPipelineResolveParameter.getChainedIndex());
        pipelineDefinition.setSchedTaskDefineList(pipelineDefine.getSchedTaskDefineList());
        return pipelineDefinition;
    }

    @Override
    public List<TaskInstance> queryTaskNodeListExecState(String projectId, String instanceId) {
        // http://pre-bmr.scheduler.bilibili.co/dolphinscheduler/projects/12969915754848/process-instances/41216/tasks
        String url = UrlBuilder.ofHttp(BASE_URL)
                .addPath(String.format("/projects/%s/process-instances/%s/tasks", projectId, instanceId))
                .build();

        log.info("query task exec state url is {}", url);
        String respStr = HttpRequest.get(url)
                .header(Constants.TOKEN, MDC.get(Constants.DOLPHIN_SCHEDULER_TOKEN_KEY))
                .execute().body();

        TasksExecDetailResp resp = JSONUtil.toBean(respStr, TasksExecDetailResp.class);
        BaseRespUtil.checkDolphinSchedulerResp(resp);

        return resp.getData().getTaskList();
    }

    private Optional<ProjectDefine> queryProjectByName(String projectName) {
        String url = UrlBuilder.ofHttp(BASE_URL).addPath(String.format("/projects/list")).build();
        String result = HttpRequest.get(url)
                .header(Header.CONTENT_TYPE, "application/json")
                .header("token", adminToken)
                .timeout(20000)// 超时，毫秒
                .execute().body();

        ProjectResp projectResp = JSONUtil.toBean(result, ProjectResp.class);
        if (projectResp.getCode() != 0) {
            throw new RequesterException(projectResp == null ? "response is null!" : projectResp.getMsg());
        }

        Optional<ProjectDefine> projectDefine = projectResp.getData().stream().filter(x -> x.getName().equals(projectName.trim())).findFirst();
        return projectDefine;
    }


    private DagData getDagDataBycode(String projectCode, String pipelineCode) {
        String url = UrlBuilder.ofHttp(BASE_URL).addPath(String.format("/projects/%s/process-definition/%s", projectCode, pipelineCode)).build();
        String respStr = HttpRequest.get(url).header(Constants.TOKEN, adminToken).execute().body();
        PipelineDefinitionResp resp = JSONUtil.toBean(respStr, PipelineDefinitionResp.class);
        BaseRespUtil.checkDolphinSchedulerResp(resp);
        DagData dagData = resp.getData();
        return dagData;
    }

    private DagData getDagDataByName(String projectCode, String pipelineName) {
        String url = UrlBuilder.ofHttp(BASE_URL).addPath(String.format("projects/%s/process-definition/query-by-name", projectCode)).build();
        String result = HttpRequest.get(url)
                .header(Header.CONTENT_TYPE, "application/json")
                .header("token", adminToken)
                .form("name", pipelineName.trim())
                .timeout(20000)// 超时，毫秒
                .execute().body();
        PipelineDefinitionResp resp = JSONUtil.toBean(result, PipelineDefinitionResp.class);
        BaseRespUtil.checkDolphinSchedulerResp(resp);
        DagData dagData = resp.getData();
        return dagData;
    }

}
