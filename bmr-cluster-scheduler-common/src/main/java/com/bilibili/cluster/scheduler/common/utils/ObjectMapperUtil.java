package com.bilibili.cluster.scheduler.common.utils;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalTimeSerializer;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Objects;

/**
 * @description: json转换mapper的配置和获取类
 * @Date: 2023/7/18 16:46
 * @Author: xiexieliangjie
 */

@Slf4j
public class ObjectMapperUtil {

    private final static SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public static ObjectMapper createObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(MapperFeature.USE_STD_BEAN_NAMING);
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);

        JavaTimeModule javaTimeModule = new JavaTimeModule();

        javaTimeModule.addSerializer(Date.class, new JsonSerializer<Date>() {

            @Override
            public void serialize(Date value, JsonGenerator gen, SerializerProvider provider) throws IOException {
                provider.defaultSerializeValue(DateTimeFormatter.ISO_INSTANT.format(value.toInstant()), gen);
            }
        });

        javaTimeModule.addSerializer(LocalDate.class, new LocalDateSerializer(DateTimeFormatter.ISO_LOCAL_DATE));
        javaTimeModule.addSerializer(LocalTime.class, new LocalTimeSerializer(DateTimeFormatter.ISO_LOCAL_TIME));
        javaTimeModule.addSerializer(LocalDateTime.class,
                new LocalDateTimeSerializer(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        // javaTimeModule.addSerializer(String.class, new JsonSerializer<String>() {
        // @Override
        // public void serialize(String value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        // if (value != null) {
        // String encodedValue = value.replaceAll("<", StringUtils.SPACE).replaceAll(">", StringUtils.SPACE);
        // gen.writeString(encodedValue);
        // }
        // }
        // });

        javaTimeModule.addSerializer(BigDecimal.class, new JsonSerializer<BigDecimal>() {

            @Override
            public void serialize(BigDecimal value, JsonGenerator gen,
                                  SerializerProvider serializers) throws IOException {
                if (value != null) {
                    gen.writeString(value.toPlainString());
                }
            }
        });

        javaTimeModule.addSerializer(Date.class, new JsonSerializer<Date>() {

            @Override
            public void serialize(Date value, JsonGenerator gen, SerializerProvider provider) throws IOException {
                provider.defaultSerializeValue(format.format(value), gen);
            }
        });

        javaTimeModule.addSerializer(Double.class, new JsonSerializer<Double>() {

            @Override
            public void serialize(Double value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
                if (value != null) {
                    gen.writeNumber(value.doubleValue());
                }
            }
        });
        javaTimeModule.addSerializer(Long.class, new JsonSerializer<Long>() {

            @Override
            public void serialize(Long value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
                if (value != null) {
                    gen.writeNumber(value.longValue());
                }
            }
        });
        javaTimeModule.addSerializer(Float.class, new JsonSerializer<Float>() {

            @Override
            public void serialize(Float value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
                if (value != null) {
                    gen.writeNumber(value.floatValue());
                }
            }
        });
        javaTimeModule.addSerializer(Integer.class, new JsonSerializer<Integer>() {

            @Override
            public void serialize(Integer value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
                if (value != null) {
                    gen.writeNumber(value.intValue());
                }
            }
        });

        javaTimeModule.addDeserializer(LocalDateTime.class,
                new LocalDateTimeDeserializer(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

        javaTimeModule.addDeserializer(Date.class, new JsonDeserializer<Date>() {

            @Override
            public Date deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
                if (Objects.isNull(p.getText())) {
                    return null;
                }

                return Date.from(Instant.parse(p.getText()));
            }
        });
        javaTimeModule.addDeserializer(String.class, new JsonDeserializer<String>() {

            @Override
            public String deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
                String value = p.getText();
                if (Objects.isNull(value)) {
                    return null;
                }
                // 身份证尾号大写
                if (("ssn").equals(p.getCurrentName())) {
                    value = value.toUpperCase();
                }
                value = value.trim();
                if ("null".equals(value)) {
                    return null;
                }
                return value;
            }
        });

        javaTimeModule.addDeserializer(Double.class, new JsonDeserializer<Double>() {

            @Override
            public Double deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
                String value = p.getText();
                if (value == null || "-".equals(value)) {
                    return null;
                }
                try {
                    return Double.valueOf(value);
                } catch (NumberFormatException e) {
                    log.warn("error double");
                    return null;
                }
            }
        });

        mapper.registerModule(javaTimeModule);
        return mapper;
    }

    public static ObjectMapper getCommonObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true);
        // mapper.setPropertyNamingStrategy(PropertyNamingStrategy.CAMEL_CASE_TO_LOWER_CASE_WITH_UNDERSCORES);
        // mapper.setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE);
        mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        return mapper;
    }

    public static ObjectMapper getYamlObjectMapper() {
        return new ObjectMapper(new YAMLFactory());
    }
}
