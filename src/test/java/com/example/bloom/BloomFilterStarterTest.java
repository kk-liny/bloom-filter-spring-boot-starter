package com.example.bloom;

import com.example.bloom.annotation.BloomFilterLoader;
import com.example.bloom.core.BloomDataLoaderStrategy;
import com.example.bloom.core.BloomFilterManager;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.context.annotation.Import;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author 86183
 * @description 测试类
 */
// 指定了要加载哪些配置类来构建测试上下文
@SpringBootTest(classes = {BloomFilterStarterTest.TestConfig.class})
// 启动自动配置机制，告诉 Spring Boot 应该扫描类路径，寻找并应用符合条件的自动配置类注入
@EnableAutoConfiguration
public class BloomFilterStarterTest {

    // 导入要实例化到容器中的类
    @Import({RedisProperties.class, TestDataLoader.class})
    static class TestConfig {}

    @Component
    @BloomFilterLoader("test-filter")
    public static class TestDataLoader implements BloomDataLoaderStrategy {
        @Override
        public List<String> loadData() {
            return Arrays.asList("user1", "user2", "user3");
        }
    }

    @Resource
    private BloomFilterManager bloomFilterManager;

    @Test
    public void testBloomFilterCreatedAndConfigured() {
        // 测试布隆过滤器是否正确创建
        assertNotNull(bloomFilterManager, "BloomFilterManager should be created");

        // 测试添加和检查功能
        bloomFilterManager.add("test-filter", "newUser");

        assertTrue(bloomFilterManager.mightContain("test-filter", "user1"));

        // 测试不存在的值
        assertFalse(bloomFilterManager.mightContain("test-filter", "nonExistingUser"));
    }
}
