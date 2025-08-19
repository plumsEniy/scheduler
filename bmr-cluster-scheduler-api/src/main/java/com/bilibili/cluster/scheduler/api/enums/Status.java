/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bilibili.cluster.scheduler.api.enums;

import org.springframework.context.i18n.LocaleContextHolder;

import java.util.Locale;
import java.util.Optional;

/**
 * status enum      // todo #4855 One category one interval
 */
public enum Status {

    SUCCESS(0, "success", "成功"),

    INTERNAL_SERVER_ERROR_ARGS(10000, "Internal Server Error: {0}", "服务端异常: {0}"),

    REQUEST_PARAMS_NOT_VALID_ERROR(10001, "request parameter {0} is not valid", "请求参数[{0}]无效"),
    TASK_TIMEOUT_PARAMS_ERROR(10002, "task timeout parameter is not valid", "任务超时参数无效"),
    USER_NAME_EXIST(10003, "user name already exists", "用户名已存在"),
    USER_NAME_NULL(10004, "user name is null", "用户名不能为空"),
    USER_NOT_EXIST(10010, "user {0} not exists", "用户[{0}]不存在"),

    DATASOURCE_EXIST(10015, "data source name already exists", "数据源名称已存在"),
    DATASOURCE_CONNECT_FAILED(10016, "data source connection failed", "建立数据源连接失败"),
    PROCESS_INSTANCE_NOT_EXISTS(10020, "process instance {0} does not exist", "任务实例[{0}]不存在"),

    GRPC_EXCEPTION(10021, "the grpc service of {0} failed", "[{0}]的grpc服务发生异常"),

    SCHEDULE_STATUS_UNKNOWN(10026, "unknown status: {0}", "未知状态: {0}");

    private final int code;
    private final String enMsg;
    private final String zhMsg;

    Status(int code, String enMsg, String zhMsg) {
        this.code = code;
        this.enMsg = enMsg;
        this.zhMsg = zhMsg;
    }

    public int getCode() {
        return this.code;
    }

    public String getMsg() {
        if (Locale.SIMPLIFIED_CHINESE.getLanguage().equals(LocaleContextHolder.getLocale().getLanguage())) {
            return this.zhMsg;
        } else {
            return this.enMsg;
        }
    }

    /**
     * Retrieve Status enum entity by status code.
     */
    public static Optional<Status> findStatusBy(int code) {
        for (Status status : Status.values()) {
            if (code == status.getCode()) {
                return Optional.of(status);
            }
        }
        return Optional.empty();
    }
}
