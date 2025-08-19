package com.bilibili.cluster.scheduler.dao.datasource;


import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

import java.lang.reflect.Method;

@Service
@Aspect
@Order(1)
public class DataSourceAspect {
    /**
     * 拦截目标方法，获取由@DataSource指定的数据源标识，设置到线程存储中以便切换数据源
     *
     * @param point
     * @throws Exception
     */
//    @Before("execution(* cn.com.base.admin.dao.*.*.*(..))")
    @Before(value = "@annotation(com.bilibili.cluster.scheduler.dao.datasource.DataSource)")
    public void intercept(JoinPoint point) throws Exception {
        Class<?> target = point.getTarget().getClass();
        MethodSignature signature = (MethodSignature) point.getSignature();
        // 默认使用目标类型的注解，如果没有则使用其实现接口的注解
        for (Class<?> clazz : target.getInterfaces()) {
            resolveDataSource(clazz, signature.getMethod());
        }
        resolveDataSource(target, signature.getMethod());
    }

    @After(value = "@annotation(com.bilibili.cluster.scheduler.dao.datasource.DataSource)")
    public void after() throws Throwable {
        DynamicDataSourceHolder.clearDataSource();
    }

    /**
     * 提取目标对象方法注解和类型注解中的数据源标识
     *
     * @param clazz
     * @param method
     */
    private void resolveDataSource(Class<?> clazz, Method method) {
        try {
            Class<?>[] types = method.getParameterTypes();
            // 默认使用类型注解
            if (clazz.isAnnotationPresent(DataSource.class)) {
                DataSource source = clazz.getAnnotation(DataSource.class);
                DataSourceKey dataSourceKey = source.dataSourceKey();
                DynamicDataSourceHolder.setDataSource(dataSourceKey);
            }
            // 方法注解可以覆盖类型注解
            Method m = clazz.getMethod(method.getName(), types);
            if (m != null && m.isAnnotationPresent(DataSource.class)) {
                DataSource source = m.getAnnotation(DataSource.class);
                DataSourceKey dataSourceKey = source.dataSourceKey();
                DynamicDataSourceHolder.setDataSource(dataSourceKey);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}

