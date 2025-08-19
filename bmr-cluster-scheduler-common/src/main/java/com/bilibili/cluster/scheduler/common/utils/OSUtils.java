package com.bilibili.cluster.scheduler.common.utils;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.RuntimeMXBean;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.GlobalMemory;
import oshi.hardware.HardwareAbstractionLayer;

/**
 * os utils
 */
public class OSUtils {

    private static final Logger logger = LoggerFactory.getLogger(OSUtils.class);

    private static final SystemInfo SI = new SystemInfo();
    public static final String TWO_DECIMAL = "0.00";

    /**
     * return -1 when the function can not get hardware env info
     * e.g {@link OSUtils#loadAverage()} {@link OSUtils#cpuUsage()}
     */
    public static final double NEGATIVE_ONE = -1;

    private static final HardwareAbstractionLayer hal = SI.getHardware();
    private static long[] prevTicks = new long[CentralProcessor.TickType.values().length];
    private static long prevTickTime = 0L;
    private static double cpuUsage = 0.0D;

    private OSUtils() {
        throw new UnsupportedOperationException("Construct OSUtils");
    }

    /**
     * Initialization regularization, solve the problem of pre-compilation performance,
     * avoid the thread safety problem of multi-thread operation
     */
    private static final Pattern PATTERN = Pattern.compile("\\s+");

    /**
     * get memory usage
     * Keep 2 decimal
     *
     * @return percent %
     */
    public static double memoryUsage() {
        GlobalMemory memory = hal.getMemory();
        double memoryUsage = (memory.getTotal() - memory.getAvailable()) * 1.0 / memory.getTotal();

        DecimalFormat df = new DecimalFormat(TWO_DECIMAL);
        df.setRoundingMode(RoundingMode.HALF_UP);
        return Double.parseDouble(df.format(memoryUsage));
    }

    /**
     * get disk usage
     * Keep 2 decimal
     *
     * @return disk free size, unit: GB
     */
    public static double diskAvailable() {
        File file = new File(".");
        long freeSpace = file.getFreeSpace(); // unallocated / free disk space in bytes.

        double diskAvailable = freeSpace / 1024.0 / 1024 / 1024;

        DecimalFormat df = new DecimalFormat(TWO_DECIMAL);
        df.setRoundingMode(RoundingMode.HALF_UP);
        return Double.parseDouble(df.format(diskAvailable));
    }

    /**
     * get available physical memory size
     * <p>
     * Keep 2 decimal
     *
     * @return available Physical Memory Size, unit: G
     */
    public static double availablePhysicalMemorySize() {
        GlobalMemory memory = hal.getMemory();
        double availablePhysicalMemorySize = memory.getAvailable() / 1024.0 / 1024 / 1024;

        DecimalFormat df = new DecimalFormat(TWO_DECIMAL);
        df.setRoundingMode(RoundingMode.HALF_UP);
        return Double.parseDouble(df.format(availablePhysicalMemorySize));
    }

    /**
     * load average
     *
     * @return load average
     */
    public static double loadAverage() {
        double loadAverage;
        try {
            OperatingSystemMXBean osBean = ManagementFactory.getPlatformMXBean(OperatingSystemMXBean.class);
            loadAverage = osBean.getSystemLoadAverage();
        } catch (Exception e) {
            logger.error("get operation system load average exception, try another method ", e);
            loadAverage = hal.getProcessor().getSystemLoadAverage(1)[0];
            if (Double.isNaN(loadAverage)) {
                return NEGATIVE_ONE;
            }
        }
        DecimalFormat df = new DecimalFormat(TWO_DECIMAL);
        df.setRoundingMode(RoundingMode.HALF_UP);
        return Double.parseDouble(df.format(loadAverage));
    }

    /**
     * get cpu usage
     *
     * @return cpu usage
     */
    public static double cpuUsage() {
        CentralProcessor processor = hal.getProcessor();

        // Check if > ~ 0.95 seconds since last tick count.
        long now = System.currentTimeMillis();
        if (now - prevTickTime > 950) {
            // Enough time has elapsed.
            cpuUsage = processor.getSystemCpuLoadBetweenTicks(prevTicks);
            prevTickTime = System.currentTimeMillis();
            prevTicks = processor.getSystemCpuLoadTicks();
        }

        if (Double.isNaN(cpuUsage)) {
            return NEGATIVE_ONE;
        }

        DecimalFormat df = new DecimalFormat(TWO_DECIMAL);
        df.setRoundingMode(RoundingMode.HALF_UP);
        return Double.parseDouble(df.format(cpuUsage));
    }

    /**
     * get process id
     *
     * @return process id
     */
    public static int getProcessID() {
        RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
        return Integer.parseInt(runtimeMXBean.getName().split("@")[0]);
    }

    /**
     * Check memory and cpu usage is overload the given thredshod.
     *
     * @param maxCpuLoadAvg  maxCpuLoadAvg
     * @param reservedMemory reservedMemory
     * @return True, if the cpu or memory exceed the given thredshod.
     */
    public static Boolean isOverload(double maxCpuLoadAvg, double reservedMemory) {
        // system load average
        double loadAverage = loadAverage();
        // system available physical memory
        double availablePhysicalMemorySize = availablePhysicalMemorySize();
        if (loadAverage > maxCpuLoadAvg || availablePhysicalMemorySize < reservedMemory) {
            logger.warn(
                    "Current cpu load average {} is too high or available memory {}G is too low, under max.cpuLoad.avg={} and reserved.memory={}G",
                    loadAverage, availablePhysicalMemorySize, maxCpuLoadAvg, reservedMemory);
            return true;
        }
        return false;
    }

}
