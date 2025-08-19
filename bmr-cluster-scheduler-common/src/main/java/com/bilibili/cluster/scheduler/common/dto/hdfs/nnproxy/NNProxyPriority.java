package com.bilibili.cluster.scheduler.common.dto.hdfs.nnproxy;

import com.google.common.base.Preconditions;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@AllArgsConstructor
public enum NNProxyPriority implements Comparable<NNProxyPriority> {

    LOW(1),

    MEDIUM(2),

    HIGH(3),
    ;

    @Getter
    int priority;

    private static Map<Integer, NNProxyPriority> VALUE_MAP = Arrays.stream(values())
            .collect(Collectors.toMap(NNProxyPriority::getPriority, Function.identity()));

    public static void checkPriority(int priority) {
        Preconditions.checkState(VALUE_MAP.containsKey(priority), "优先级不存在｜不支持: " + priority);
    }

    public static boolean isValid(int priority) {
        return VALUE_MAP.containsKey(priority);
    }

    public static NNProxyPriority getByValue(int priority) {
        return VALUE_MAP.get(priority);
    }
}
