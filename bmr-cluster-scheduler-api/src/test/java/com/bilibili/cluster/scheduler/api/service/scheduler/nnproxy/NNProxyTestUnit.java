package com.bilibili.cluster.scheduler.api.service.scheduler.nnproxy;

import cn.hutool.json.JSONUtil;
import com.bilibili.cluster.scheduler.api.service.bmr.resource.BmrResourceService;
import com.bilibili.cluster.scheduler.api.service.bmr.resource.BmrResourceServiceImpl;
import com.bilibili.cluster.scheduler.api.service.flow.prepare.factory.nnproxy.NNProxyDeployFlowPrepareGenerateFactory;
import com.bilibili.cluster.scheduler.common.Constants;
import com.bilibili.cluster.scheduler.common.dto.bmr.resource.ComponentNodeDetail;
import com.bilibili.cluster.scheduler.common.dto.bmr.resource.req.QueryComponentNodeListReq;
import com.bilibili.cluster.scheduler.common.dto.hdfs.nnproxy.NNProxyPriority;
import com.bilibili.cluster.scheduler.common.utils.LocalDateFormatterUtils;
import com.bilibili.cluster.scheduler.common.utils.StageSplitUtil;
import com.google.common.base.Preconditions;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

public class NNProxyTestUnit {

    @Test
    public void test01() {
        Map<NNProxyPriority, Collection<Integer>> priorityWithComponentIdList = new TreeMap<>();

        boolean isSortValue = true;

        Function func = isSortValue ?  key -> new TreeSet<>() :
                key -> new ArrayList<>();

        priorityWithComponentIdList.computeIfAbsent(NNProxyPriority.HIGH, func).add(1);
        priorityWithComponentIdList.computeIfAbsent(NNProxyPriority.HIGH, func).add(4);

        priorityWithComponentIdList.computeIfAbsent(NNProxyPriority.LOW, func).add(3);
        priorityWithComponentIdList.computeIfAbsent(NNProxyPriority.LOW, func).add(5);

        priorityWithComponentIdList.computeIfAbsent(NNProxyPriority.HIGH, func).add(2);
        priorityWithComponentIdList.computeIfAbsent(NNProxyPriority.HIGH, func).add(3);

        priorityWithComponentIdList.computeIfAbsent(NNProxyPriority.MEDIUM, func).add(6);
        priorityWithComponentIdList.computeIfAbsent(NNProxyPriority.MEDIUM, func).add(7);

        priorityWithComponentIdList.computeIfAbsent(NNProxyPriority.LOW, func).add(32);
        priorityWithComponentIdList.computeIfAbsent(NNProxyPriority.LOW, func).add(55);
        priorityWithComponentIdList.computeIfAbsent(NNProxyPriority.HIGH, func).add(5);

        System.out.println(priorityWithComponentIdList);

        final Stack<Object> objects = new Stack<>();

        final List<Map.Entry<NNProxyPriority, Collection<Integer>>> collect = priorityWithComponentIdList.entrySet().stream()
               // .sorted(Comparator.comparingInt(e -> e.getValue().size()))
               //  .sorted((x,y) -> y.getValue().size() - x.getValue().size())
               // .sorted(Comparator.comparing(e -> e.getValue().size(), Comparator.reverseOrder()))
                .collect(Collectors.toList());

        collect.forEach(System.out::println);

        collect.forEach(System.out::println);

    }

    @Test
    public void test02() {
        String nowFmt = LocalDateFormatterUtils.getNowDefaultFmt();
        System.out.println(nowFmt);
        final String before = LocalDateFormatterUtils.format(Constants.FMT_DATE_TIME,
                LocalDateTime.now().minus(1, ChronoUnit.MINUTES));
        System.out.println(nowFmt.compareTo(before));
        System.out.println(before.compareTo(nowFmt));

        System.out.println(generateNodeList(20, 30, 2, "jscs-bigdata-nnproxy-"));


    }

    @Test
    public void test03() {
        int size = 3;

        // result map
        Map<String, Set<String>> stageWithNodeList = new LinkedHashMap<>();

        Function func1 = stage -> Integer.parseInt(stage.toString());
        Function func2 = stage -> Integer.parseInt(stage.toString()) + 1;
        Function func3 = stage -> Integer.parseInt(stage.toString()) + 3;

        Map<Integer, Function<String, Integer>> funcMap = new HashMap<>();

        // 存在两个优先等级
        if (size == 2) {
            funcMap.put(1, func1);
            funcMap.put(2, func3);
        } else {
            funcMap.put(1, func1);
            funcMap.put(2, func2);
            funcMap.put(3, func3);
        }

        final AtomicInteger index = new AtomicInteger(0);
        List<Integer> percentList = Arrays.asList(10, 50, 100);

        Map<NNProxyPriority, List<Long>> priorityWithComponentIdList = new TreeMap<>();
        Function func = k -> new ArrayList<Long>();
        Map<Long, List<String>> componentIdToNodeList = new HashMap<>();

        priorityWithComponentIdList.computeIfAbsent(NNProxyPriority.HIGH, func).add(1l);
        componentIdToNodeList.put(1l, generateNodeList(20, 26, 1, "jscs-bigdata-nnproxy-"));
//        priorityWithComponentIdList.computeIfAbsent(NNProxyPriority.HIGH, func).add(4l);
//        componentIdToNodeList.put(4l, generateNodeList(26, 33, 4, "jscs-bigdata-nnproxy-"));

        priorityWithComponentIdList.computeIfAbsent(NNProxyPriority.LOW, func).add(3l);
        componentIdToNodeList.put(3l, generateNodeList(33, 39, 3, "jscs-bigdata-nnproxy-"));
//        priorityWithComponentIdList.computeIfAbsent(NNProxyPriority.LOW, func).add(5l);
//        componentIdToNodeList.put(5l, generateNodeList(33, 39, 5, "jscs-bigdata-nnproxy-"));

        priorityWithComponentIdList.computeIfAbsent(NNProxyPriority.HIGH, func).add(6l);
        componentIdToNodeList.put(6l, generateNodeList(40, 55, 6, "jscs-bigdata-nnproxy-"));
//        priorityWithComponentIdList.computeIfAbsent(NNProxyPriority.HIGH, func).add(8l);
//        componentIdToNodeList.put(8l, generateNodeList(52, 60, 8, "jssz-bigdata-nnproxy-"));
        priorityWithComponentIdList.computeIfAbsent(NNProxyPriority.MEDIUM, func).add(2l);
        componentIdToNodeList.put(2l, generateNodeList(61, 63, 2, "jssz-bigdata-nnproxy-"));
//        priorityWithComponentIdList.computeIfAbsent(NNProxyPriority.MEDIUM, func).add(7l);
//        componentIdToNodeList.put(7l, generateNodeList(68, 70, 7, "jssz-bigdata-nnproxy-"));
//
//        priorityWithComponentIdList.computeIfAbsent(NNProxyPriority.LOW, func).add(32l);
//        componentIdToNodeList.put(32l, generateNodeList(70, 77, 32, "jssz-bigdata-nnproxy-"));

        priorityWithComponentIdList.computeIfAbsent(NNProxyPriority.LOW, func).add(55l);
        componentIdToNodeList.put(55l, generateNodeList(80, 81, 55, "jssz-bigdata-nnproxy-"));

//        priorityWithComponentIdList.computeIfAbsent(NNProxyPriority.HIGH, func).add(36l);
//        componentIdToNodeList.put(36l, generateNodeList(81, 88, 36, "jssz-bigdata-nnproxy-"));


        size = priorityWithComponentIdList.size();
        // 存在两个优先等级
        if (size == 2) {
            funcMap.put(1, func1);
            funcMap.put(2, func3);
        } else {
            funcMap.put(1, func1);
            funcMap.put(2, func2);
            funcMap.put(3, func3);
        }

        for (Map.Entry<NNProxyPriority, List<Long>> entry : priorityWithComponentIdList.entrySet()) {
            index.incrementAndGet();
            List<Long> componentIdList = entry.getValue();
            if (CollectionUtils.isEmpty(componentIdList)) {
                continue;
            }
            for (Long componentId : componentIdList) {
                List<String> nodeList = componentIdToNodeList.get(componentId);
                if (CollectionUtils.isEmpty(nodeList)) {
                    continue;
                }
                final Map<String, Set<String>> stageMap = StageSplitUtil.buildStageMap(nodeList, percentList);
                stageMap.entrySet().stream().forEach(
                        e -> {
                            final String stageValue = e.getKey();
                            final Function<String, Integer> function = funcMap.get(index.get());
                            Preconditions.checkNotNull(function, "priority mapping func not support: " + entry.getKey());
                            final String stage = function.apply(stageValue).toString();
                            stageWithNodeList.computeIfAbsent(stage, k -> new LinkedHashSet<>()).addAll(e.getValue());
                        }
                );
            }
        }

        stageWithNodeList.entrySet().forEach(e -> {
            String stage = e.getKey();
            final Set<String> nodes = e.getValue();
            System.out.println("stage=" + stage);
            System.out.println("nodes=" + nodes);
        });

    }


    private List<String> generateNodeList(int start, int end, int index, String prefix) {

        List<String> nodeList = new ArrayList<>();
        for (int i = start; i < end; i++) {
            nodeList.add(prefix + index + "-" + i);
        }
        return nodeList;
    }

    @Test
    public void testRollbackByDNS() {
        BmrResourceServiceImpl resourceService = new BmrResourceServiceImpl();

        // resourceService.setBASE_URL("http://pre-cloud-bm.bilibili.co");
        // resourceService.setActive("pre");
        resourceService.setBASE_URL("http://bmr.bilibili.co/");
        resourceService.setActive("prod");

        QueryComponentNodeListReq req  = new QueryComponentNodeListReq();
        req.setNeedDns(true);
        //req.setClusterId(12l);
        req.setClusterId(24l);
        req.setApplicationState(null);
        final List<ComponentNodeDetail> componentNodeDetails = resourceService.queryNodeList(req);
        final Map<String, Map<String, List<String>>> dnsMap = new TreeMap<>();
        for (ComponentNodeDetail componentNodeDetail : componentNodeDetails) {
            final String componentName = componentNodeDetail.getComponentName();
            final String dns = componentNodeDetail.getDns();
            final String hostName = componentNodeDetail.getHostName();
            Preconditions.checkState(!StringUtils.isBlank(dns),
                    hostName + " dns is blank, please check");
            dnsMap.computeIfAbsent(componentName, k -> new TreeMap<>())
                    .computeIfAbsent(dns, v -> new ArrayList<>())
                    .add(hostName);
        }
        System.out.println(JSONUtil.toJsonStr(dnsMap));

        Map<Integer, List<String>> batchToHostList = new LinkedHashMap<>();
        for (Map.Entry<String, Map<String, List<String>>> componentEntry : dnsMap.entrySet()) {
            Map<String, List<String>> dnsToHostList = componentEntry.getValue();
            int index = 0;
            for (Map.Entry<String, List<String>> dnsEntry : dnsToHostList.entrySet()) {
                index++;
                batchToHostList.computeIfAbsent(index, i -> new ArrayList<>()).addAll(dnsEntry.getValue());
            }
        }

        System.out.println(JSONUtil.toJsonStr(batchToHostList));
    }



}
