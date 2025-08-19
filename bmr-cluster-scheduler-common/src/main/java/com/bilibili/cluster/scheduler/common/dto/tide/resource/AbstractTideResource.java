package com.bilibili.cluster.scheduler.common.dto.tide.resource;

import lombok.Getter;

/**
 * @description:
 * @Date: 2025/3/20 14:57
 * @Author: nizhiqiang
 */


public abstract class AbstractTideResource {

    /**
     * 预期pod数
     */
    @Getter
    final int expectedPod;

    /**
     * 良好的百分比
     */
    final Double finePercent;

    /**
     * 最低的百分比
     */
    final Double minPercent;

    /**
     * 迫不得已最差的百分比
     */
    final Double leastPercent;

    public AbstractTideResource(int expectedPod, Double finePercent, Double minPercent, Double leastPercent) {
        this.expectedPod = expectedPod;
        this.finePercent = finePercent;
        this.minPercent = minPercent;
        this.leastPercent = leastPercent;
    }

    /**
     * 是否达到预期
     *
     * @return
     */
    public abstract boolean isAlready();

    /**
     * 是否达到良好
     *
     * @return
     */
    public abstract boolean isFine();

    /**
     * 是否达到最低
     *
     * @return
     */
    public abstract boolean isMin();


    /**
     * 是否最差
     * @return
     */
    public abstract boolean isLeast();

    public int getFinePodCount() {
        Double fineShrinkPodValue = Math.floor(expectedPod * finePercent);
        return fineShrinkPodValue.intValue() == 0 ? 1 : fineShrinkPodValue.intValue();
    }

    public int getMinPodCount() {
        Double minShrinkPodValue = Math.floor(expectedPod * minPercent);
        return minShrinkPodValue.intValue() == 0 ? 1 : minShrinkPodValue.intValue();
    }

    public int getLeastPodCount() {
        Double leastShrinkPodValue = Math.floor(expectedPod * leastPercent);
        return leastShrinkPodValue.intValue() == 0 ? 1 : leastShrinkPodValue.intValue();
    }
}
