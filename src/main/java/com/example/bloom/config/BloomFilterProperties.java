package com.example.bloom.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * @author 86183
 * @description 读取配置属性
 */
@Data
@Component
@ConfigurationProperties(prefix = "bloom")
public class BloomFilterProperties {

    private boolean enabled;
    private Map<String, BloomFilterConfig> filterConfigMap;

    @Data
    public static class BloomFilterConfig {
        private long expectedInsertions;
        private double falseProbability;
        private String redisKey;
        private String redisDataSourceType;
    }
}
