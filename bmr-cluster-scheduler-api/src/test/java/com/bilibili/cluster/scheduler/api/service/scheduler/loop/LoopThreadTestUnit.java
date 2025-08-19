package com.bilibili.cluster.scheduler.api.service.scheduler.loop;

import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.RandomUtil;
import com.bilibili.cluster.scheduler.common.Constants;
import com.bilibili.cluster.scheduler.common.utils.LocalDateFormatterUtils;
import org.junit.Test;

public class LoopThreadTestUnit {




    @Test
    public void testSafeSleep() {
        System.out.println(LocalDateFormatterUtils.getNowMilliFmt());

        final int randomInt = RandomUtil.getRandom().nextInt(1_00);
        ThreadUtil.safeSleep(randomInt);

        System.out.println(LocalDateFormatterUtils.getNowMilliFmt());


        long waitTs = Constants.ONE_SECOND;
       //  log.info("current task holder the lock {}, wait ts {}, than start running....", lock, waitTs);
        ThreadUtil.safeSleep(waitTs);

        System.out.println(LocalDateFormatterUtils.getNowMilliFmt());
    }





}
