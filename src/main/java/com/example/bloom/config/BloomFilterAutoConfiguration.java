package com.example.bloom.config;

import com.example.bloom.core.BloomFilterManager;
import com.example.bloom.core.impl.RedisBloomFilterManager;
import com.example.bloom.factory.BloomFilterLoaderFactory;
import lombok.extern.slf4j.Slf4j;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.redisson.config.SingleServerConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author 86183
 * @description 装配BloomFilterLoaderFactory 和 BloomFilterManager
 */
@Slf4j
// 检查是否包含 RedissonClient 这个依赖
@ConditionalOnClass(RedissonClient.class)
@Configuration
@EnableConfigurationProperties(BloomFilterProperties.class)
@ConditionalOnProperty(prefix = "bloom", name = "enabled", havingValue = "true")
public class BloomFilterAutoConfiguration {

    /**
     * 创建 RedissonClient 客户端
     * 优先使用 Spring 配置的 Redis 属性，如果没有则使用默认配置
     *
     * @param redisProperties Spring Boot 的 Redis 配置属性
     * @return RedissonClient 实例
     */
    @Bean
    @ConditionalOnMissingBean
    public RedissonClient redissonClient(
            @Autowired(required = false) RedisProperties redisProperties) {

        Config config = new Config();
        SingleServerConfig serverConfig = config.useSingleServer();

        if (redisProperties == null) {
            log.info("Redis not configured, using default: {}", "redis://localhost:6379");
            serverConfig.setAddress("redis://localhost:6379");
            return Redisson.create(config);
        }

        log.info("Using Redis configuration: host={}, port={}", redisProperties.getHost(), redisProperties.getPort());

        serverConfig.setAddress("redis://" + redisProperties.getHost() + ":" + redisProperties.getPort());

        if (redisProperties.getPassword() != null && !redisProperties.getPassword().isEmpty()) {
            serverConfig.setPassword(redisProperties.getPassword());
        }

        serverConfig.setDatabase(redisProperties.getDatabase());

        if (redisProperties.getConnectTimeout() != null) {
            serverConfig.setConnectTimeout(
                    (int) redisProperties.getConnectTimeout().toMillis());
        }

        return Redisson.create(config);
    }

    /**
     * 创建布隆过滤器加载器工厂
     *
     * @param context Spring 应用上下文
     * @return BloomFilterLoaderFactory 实例
     */
    @Bean
    public BloomFilterLoaderFactory bloomFilterLoaderFactory(ApplicationContext context) {
        return new BloomFilterLoaderFactory(context);
    }

    /**
     * 创建布隆过滤器管理器
     *
     * @param properties     布隆过滤器属性配置
     * @param redissonClient Redisson 客户端
     * @param loaderFactory  布隆过滤器加载器工厂
     * @return BloomFilterManager 实例
     */
    @Bean
    public BloomFilterManager bloomFilterManager(
            BloomFilterProperties properties,
            RedissonClient redissonClient,
            BloomFilterLoaderFactory loaderFactory) {
        return new RedisBloomFilterManager(properties, redissonClient, loaderFactory);
    }

    // TODO 增加一个没有RedissonClient的BloomFilterManager 依赖于Redis 和 Redisson 太sb了
    // 问题：没有Redis，数据存在哪？本地内存？

    // TODO 抽出日志？如果不需要扩展的话，就现在这点日志量感觉没必要
}
