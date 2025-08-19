
package com.bilibili.cluster.scheduler.api.bean;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Slf4j
@Component
public class SpringApplicationContext implements ApplicationContextAware, AutoCloseable {

    private static ApplicationContext applicationContext;

    private static String env;

    public static String getEnv() {
        return env;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        SpringApplicationContext.applicationContext = applicationContext;
        env = applicationContext.getEnvironment().getActiveProfiles()[0];
        log.info("application context env is {}.", env);
    }

    /**
     * Close this application context, destroying all beans in its bean factory.
     */
    @Override
    public void close() {
        ((AbstractApplicationContext) applicationContext).close();
    }

    public static <T> T getBean(Class<T> requiredType) {
        if (Objects.isNull(applicationContext)) {
            return null;
        }
        return applicationContext.getBean(requiredType);
    }
}
