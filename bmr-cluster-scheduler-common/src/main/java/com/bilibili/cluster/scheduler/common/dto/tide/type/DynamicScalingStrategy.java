package com.bilibili.cluster.scheduler.common.dto.tide.type;


import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum DynamicScalingStrategy {

    /**
     * 先扩容后缩容
     */
    FIRST_EXPAND_THEN_SHRINK("FIRST_EXPAND_THEN_SHRINK", "先扩容后缩容"),
    /**
     * 先缩容后扩容
     */
    FIRST_SHRINK_THEN_EXPAND("FIRST_SHRINK_THEN_EXPAND", "先缩容后扩容"),
    /**
     * Yarn 潮汐调度
     */
    YARN_TIDAL_SCHEDULE("YARN_TIDAL_SCHEDULE", "Yarn 潮汐调度"),
    ;
    @EnumValue
    @JsonValue
    private final String code;
    private final String desc;


    /**
     * 根据 code 获取枚举
     */
    public static DynamicScalingStrategy getByCode(String code) {
        for (DynamicScalingStrategy value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        throw new IllegalArgumentException("code: " + code + " not exists");
    }
}
