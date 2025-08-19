package com.bilibili.cluster.scheduler.common.utils;

import com.bilibili.cluster.scheduler.common.Constants;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.fasterxml.jackson.databind.type.MapType;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;

/**
 * @description: json转换的工具类
 * @Date: 2023/7/18 15:58
 * @Author: xiexieliangjie
 */
@Slf4j
public class ObjectTransferUtils {

    @Getter
    private static ObjectMapper mapper = ObjectMapperUtil.getCommonObjectMapper();

    @Getter
    private static ObjectMapper yamlMapper = ObjectMapperUtil.getYamlObjectMapper();


    public static <T> T jsonToObject(String json, Class<T> tClass) throws Exception {
        T t = mapper.readValue(json, tClass);
        return t;
    }

    public static <T> T jsonToObject(String json, TypeReference<T> typeReference) throws Exception {
        T t = mapper.convertValue(json, typeReference);
        return t;
    }

    public static <T> T yamlToObject(String yaml, TypeReference<T> typeReference) throws Exception {
        T t = yamlMapper.readValue(yaml, typeReference);
        return t;
    }

    public static String yamlObjectToStr(Object object) {
        try {
            return yamlMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("convert yaml to object error, error message is " + e.getMessage(), e);
        }
    }

    public static String objectToJson(Object o) {
        try {
            String json = mapper.writeValueAsString(o);
            return json;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return Constants.EMPTY_STRING;
    }

    public static <K, V> Map<K, V> objectToMap(String json, Class<? extends Map> mapClass,
                                               Class<? extends K> kClass, Class<? extends V> vClass) throws Exception {
        MapType mapType = mapper.getTypeFactory().constructMapType(mapClass, kClass, vClass);
        Map<K, V> valueMap = mapper.readValue(json, mapType);
        return valueMap;
    }

    public static <E> List<E> objectToList(String json, Class<? extends List> listClass,
                                           Class<? extends E> eClass) throws Exception {
        CollectionType collectionType = mapper.getTypeFactory().constructCollectionType(listClass, eClass);
        List<E> valueMap = mapper.readValue(json, collectionType);
        return valueMap;
    }
}
