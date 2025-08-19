package com.bilibili.cluster.scheduler.api.tools;

import org.apache.commons.lang3.StringUtils;

import java.util.*;

/**
 * @description: saber参数合并工具
 * @Date: 2024/2/26 17:52
 * @Author: nizhiqiang
 */
public class MergePropsUtil {

    /**
     * --customconfig参数覆盖
     * @param beforeCustomProps
     * @param afterCustomProps
     * @param props
     * @return
     */
    public static String coverProps(String beforeCustomProps, String afterCustomProps, String props) {
        List<String> afterPropsList;
        if(StringUtils.isBlank(afterCustomProps)){
            afterPropsList = new ArrayList<>();
        }else{
            afterPropsList = Arrays.asList(afterCustomProps.split("\\s+"));
        }
        int afterPropIndex = afterPropsList.indexOf(props);
        String afterProp = afterPropIndex == -1 || afterPropIndex == afterPropsList.size() - 1 ? "" : afterPropsList.get(afterPropsList.indexOf(props) + 1);
        if (beforeCustomProps.indexOf(props) == -1)
            if (afterProp.isEmpty()) {
                return beforeCustomProps;
            } else {
                return beforeCustomProps + " " + props + " " + afterProp;
            }
        List<String> beforePropsList = new ArrayList<>(Arrays.asList(beforeCustomProps.split("\\s+")));
        int beforePropsIndex = beforePropsList.indexOf(props);
        if (afterProp.isEmpty()) {
            beforePropsList.remove(beforePropsIndex);
            beforePropsList.remove(beforePropsIndex);
        } else {
            beforePropsList.set(beforePropsList.indexOf(props) + 1, afterProp);
        }
        StringBuffer output = new StringBuffer();
        for (String s : beforePropsList) {
            output.append(s + " ");
        }
        if (output.length() != 0) {
            output.deleteCharAt(output.length() - 1);
        }
        return output.toString();
    }

    /**
     * --customconfig参数合并
     * @param beforeCustomProps
     * @param afterCustomProps
     * @param props
     * @return
     */
    public static String mergeProps(String beforeCustomProps, String afterCustomProps, String props) {
//        afterprops没有 --customconfig 直接返回beforeconfig
        if (StringUtils.isBlank(afterCustomProps) || afterCustomProps.indexOf(props) == -1)
            return beforeCustomProps;

        List<String> afterPropsList = Arrays.asList(afterCustomProps.split("\\s+"));
        int afterPropIndex = afterPropsList.indexOf(props);
        String afterProp = afterPropIndex == -1 || afterPropIndex == afterPropsList.size() - 1 ? "" : afterPropsList.get(afterPropIndex + 1);
//       如果 before props中没有--customconfig   直接在beforeconfig后追加afterprops中的--customconfig
        if (beforeCustomProps.indexOf(props) == -1)
            return beforeCustomProps + " " + props + " " + afterProp;
        List<String> beforePropsList = Arrays.asList(beforeCustomProps.split("\\s+"));
        String beforeProp = beforePropsList.get(beforePropsList.indexOf(props) + 1);

        String updateProp = mergePropsByMap(beforeProp, afterProp);
        beforePropsList.set(beforePropsList.indexOf(props) + 1, updateProp);
        StringBuffer output = new StringBuffer();
        for (String s : beforePropsList) {
            output.append(s + " ");
        }
        if (output.length() != 0) {
            output.deleteCharAt(output.length() - 1);
        }
        return output.toString();
    }


    /**
     * 将字符串的属性取并集（后来的覆盖先前的，如果后来的没有该属性则不覆盖）
     * @param beforeProps
     * @param afterProps
     * @return
     */
    public static String mergePropsByMap(String beforeProps, String afterProps) {
        Map<String, String> beforePropsMap = convertStringToMap(beforeProps);
        Map<String, String> afterPropsMap = convertStringToMap(afterProps);

        beforePropsMap.putAll(afterPropsMap);
        return convertMapToString(beforePropsMap);
    }

    /**
     * 字符串转换成map
     * @param str   key=value,key1=value1
     * @return   <<key,value>,<key1,value1>>
     */
    public static Map<String, String> convertStringToMap(String str) {
        Map<String, String> map = new LinkedHashMap<>();
        if (!str.isEmpty()) {
            //        未判断输入数据是否符合格式
            String[] entryList = str.split(",");
            for (String s : entryList) {
                String[] split = s.split("=");
                if(split.length!=2){
                    throw new IllegalArgumentException("非法参数，key和value参数异常");
                }
                map.put(split[0], split[1]);
            }
        }
        return map;
    }

    /**
     * map拼接成字符串
     * @param map   <<key,value>,<key1,value1>>
     * @return  key=value,key1=value1
     */
    public static String convertMapToString(Map<String, String> map) {
        List<String> strList = new ArrayList<>();
        for (Map.Entry<String, String> entry : map.entrySet()) {
            strList.add(entry.getKey() + "=" + entry.getValue());
        }
        StringBuilder result = new StringBuilder();
        if (strList.size() >= 1) {
            strList.forEach(str -> result.append(str).append(","));
            result.deleteCharAt(result.length() - 1);
        }
        return result.toString();
    }
}
