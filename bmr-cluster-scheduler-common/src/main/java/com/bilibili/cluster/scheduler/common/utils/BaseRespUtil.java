package com.bilibili.cluster.scheduler.common.utils;

import com.bilibili.cluster.scheduler.common.dto.caster.resp.BaseComResp;
import com.bilibili.cluster.scheduler.common.dto.jobAgent.resp.BaseJobAgentResp;
import com.bilibili.cluster.scheduler.common.dto.scheduler.resp.BaseDolphinSchedulerResp;
import com.bilibili.cluster.scheduler.common.exception.RequesterException;
import com.bilibili.cluster.scheduler.common.response.BaseMsgResp;
import com.bilibili.cluster.scheduler.common.response.BaseResp;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;

/**
 * @description: resp工具
 * @Date: 2024/3/6 15:08
 * @Author: nizhiqiang
 */
@Slf4j
public class BaseRespUtil {

    public static void checkCommonResp(BaseResp baseResp) {
        if (Objects.isNull(baseResp) || baseResp.getCode() != 0) {
            log.error("request error : {}", baseResp);
            throw new RequesterException(baseResp == null ? "response is null!" : baseResp.getMessage());
        }
    }

    public static void checkMsgResp(BaseMsgResp baseResp) {
        if (Objects.isNull(baseResp) || baseResp.getCode() != 0) {
            log.error("request error : {}", baseResp);
            throw new RequesterException(baseResp == null ? "response is null!" : baseResp.getMsg());
        }
    }

    public static void checkComResp(BaseComResp baseResp){
        if (Objects.isNull(baseResp) || baseResp.getStatus() != 200) {
            log.error("request error : {}", baseResp);
            throw new RequesterException(baseResp == null ? "response is null!" : baseResp.getMessage());
        }
    }

    public static void checkMsgResp(BaseMsgResp baseResp, Integer normalCode) {
        if (Objects.isNull(baseResp) || baseResp.getCode() != 200) {
            log.error("request error : {}", baseResp);
            throw new RequesterException(baseResp == null ? "response is null!" : baseResp.getMsg());
        }
    }

    public static void checkDolphinSchedulerResp(BaseDolphinSchedulerResp resp) {
        //由于 startPipeline 是异步操作，因此start后马上查询的话会返回10116报文，此种情况不抛出异常
        if (Objects.isNull(resp) || (resp.getCode() != 0 && resp.getCode() != 10116)) {
            log.error("request error : {}", resp);
            throw new RequesterException(resp == null ? "response is null!" : resp.getMsg());
        }
    }

    public static void checkJobAgentResp(BaseJobAgentResp baseResp) {
        if (Objects.isNull(baseResp) || baseResp.getCode() != 200) {
            log.error("request error : {}", baseResp);
            throw new RequesterException(baseResp == null ? "response is null!" : baseResp.getMessage());
        }
    }

    public static void checkHboResp(BaseMsgResp resp) {
        if (Objects.isNull(resp) || resp.getCode() != 200) {
            log.error("request error : {}", resp);
            throw new RequesterException(resp == null ? "response is null!" : resp.getMsg());
        }
    }
}
