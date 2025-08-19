package com.bilibili.cluster.scheduler.api.configuration;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.redisson.spring.data.connection.RedissonConnectionFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Configuration
@Slf4j
public class RedissonConfig {

    @Value("${redis.nodes}")
    private String nodes;

    @Bean
    public RedissonClient redisson() throws IOException {
        List<String> clusterNodes = new ArrayList<>();
        String[] hosts = nodes.split(",");
        Config config = new Config();
        if (hosts.length == 1) {
            config.setLockWatchdogTimeout(3000);
            config.useSingleServer().setAddress("redis://" + hosts[0]);
        }
        if (hosts.length > 1) {
            for (int i = 0; i < hosts.length; i++) {
                clusterNodes.add("redis://" + hosts[i]);
            }
            config.setLockWatchdogTimeout(3000);
            config.useClusterServers()
                    .addNodeAddress(clusterNodes.toArray(new String[clusterNodes.size()]))
                    .setScanInterval(6000)
                    .setTimeout(6000)
                    .setRetryAttempts(4)
                    .setIdleConnectionTimeout(2000)
                    .setConnectTimeout(6000);
        }

        return Redisson.create(config);
    }

    @Bean
    public RedissonConnectionFactory redissonConnectionFactory(RedissonClient redisson) {
        return new RedissonConnectionFactory(redisson);
    }

    @Bean
    public RedisTemplate<String, String> redisTemplate(RedisConnectionFactory factory) {
        StringRedisTemplate template = new StringRedisTemplate(factory);
        setSerializer(template);//设置序列化工具
        template.afterPropertiesSet();
        return template;
    }

    private void setSerializer(StringRedisTemplate template) {
        Jackson2JsonRedisSerializer jackson2JsonRedisSerializer = new Jackson2JsonRedisSerializer(Object.class);
        ObjectMapper om = new ObjectMapper();
        om.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        om.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL);
        jackson2JsonRedisSerializer.setObjectMapper(om);
        template.setValueSerializer(jackson2JsonRedisSerializer);
    }
}
