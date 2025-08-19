package com.bilibili.cluster.scheduler.common.dto.tide.resource;

import lombok.Getter;
import lombok.ToString;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @description:
 * @Date: 2025/3/20 15:01
 * @Author: nizhiqiang
 */
@ToString
public class CkTideResource extends AbstractTideResource {

    private static Pattern memoryPattern = Pattern.compile("(\\d+)([GTM])");
    /**
     * 容器所需cpu 单位毫核
     */
    final double podCpuReq;

    /**
     * 容器所需内存   单位mb
     */
    final int podMemReq;

    @Getter
    long currentMemory = 0;

    @Getter
    int currentCpu = 0;

    public CkTideResource(int expectedPod, Double finePercent, Double minPercent, Double leastPercent, double podCpuReq, int podMemReq) {
        super(expectedPod, finePercent, minPercent, leastPercent);
        this.podCpuReq = podCpuReq;
        this.podMemReq = podMemReq;
    }

    @Override
    public boolean isAlready() {
        return currentMemory >= getExpectedMemory() && currentCpu >= getExpectedCpu();
    }

    @Override
    public boolean isFine() {
        return currentMemory >= getFineMemory() && currentCpu >= getFineCpu();
    }

    @Override
    public boolean isMin() {
        return currentMemory >= getMinMemory() && currentCpu >= getMinCpu();
    }

    @Override
    public boolean isLeast() {
        return currentMemory >= getLeastMemory() && currentCpu >= getLeastCpu();
    }

    public void addMemory(String memory) {
        this.currentMemory += convertStrToMemory(memory);
    }

    public void subMemory(String memory) {
        this.currentMemory -= convertStrToMemory(memory);
    }

    public void addCpu(int cpu) {
        this.currentCpu += cpu;
    }

    public void subCpu(int cpu) {
        this.currentCpu -= cpu;
    }

    public int getExpectedMemory() {
        return this.podMemReq * getExpectedPod();
    }

    public double getExpectedCpu() {
        return this.podCpuReq * getExpectedPod();
    }

    public int getFineMemory() {
        return this.podMemReq * getFinePodCount();
    }

    public double getFineCpu() {
        return this.podCpuReq * getFinePodCount();
    }

    public int getMinMemory() {
        return this.podMemReq * getMinPodCount();
    }

    public double getMinCpu() {
        return this.podCpuReq * getMinPodCount();
    }

    public int getLeastMemory() {
        return this.podMemReq * getLeastPodCount();
    }

    public double getLeastCpu() {
        return this.podCpuReq * getLeastPodCount();
    }

    private static int convertStrToMemory(String memoryStr) {
        Matcher matcher = memoryPattern.matcher(memoryStr);
        if (!matcher.find()) {
            throw new IllegalArgumentException("Invalid memory string: " + memoryStr);
        }

        int size = Integer.valueOf(matcher.group(1));
        String unit = matcher.group(2);

        switch (unit) {
            case "T":
                size *= 1024;
                break;
            case "G":
                break;
            case "M":
                size /= 1024;
                break;
            default:
                throw new IllegalArgumentException(String.format("Invalid memory unit: %s, memory str is %s", unit, memoryStr));
        }
        return size;
    }

}
