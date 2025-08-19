package com.bilibili.cluster.scheduler.dao;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableAutoConfiguration
@MapperScan(basePackages = "com.bilibili.cluster.scheduler.dao.mapper")
public class DaoConfiguration {
}
