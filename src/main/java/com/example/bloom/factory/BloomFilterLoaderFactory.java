package com.example.bloom.factory;

import com.example.bloom.annotation.BloomFilterLoader;
import com.example.bloom.core.BloomDataLoaderStrategy;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationContext;

import java.util.HashMap;
import java.util.Map;

/**
 * @author 86183
 * @description 注册所有标注了BloomFilterLoader注解的类
 */
@RequiredArgsConstructor
public class BloomFilterLoaderFactory {

    private final ApplicationContext applicationContext;
    private final Map<String, BloomDataLoaderStrategy> strategyMap = new HashMap<>();

    /**
     * 初始化所有标注了BloomFilterLoader注解的类
     */
    @PostConstruct
    public void init() {
        Map<String, Object> beansWithAnnotation =
                applicationContext.getBeansWithAnnotation(BloomFilterLoader.class);

        beansWithAnnotation.forEach((beanName, bean) -> {
            if (bean instanceof BloomDataLoaderStrategy) {
                BloomFilterLoader annotation =
                        bean.getClass().getAnnotation(BloomFilterLoader.class);
                strategyMap.put(annotation.value(), (BloomDataLoaderStrategy) bean);
            }
        });
    }

    /**
     * 根据名称获取对应的加载器
     * @param loadName 加载器的名称
     * @return 加载器对象
     */
    public BloomDataLoaderStrategy getLoader(String loadName) {
        return strategyMap.get(loadName);
    }
}
