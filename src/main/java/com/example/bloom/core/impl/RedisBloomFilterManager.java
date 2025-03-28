package com.example.bloom.core.impl;

import com.example.bloom.config.BloomFilterProperties;
import com.example.bloom.config.BloomFilterProperties.BloomFilterConfig;
import com.example.bloom.core.BloomDataLoaderStrategy;
import com.example.bloom.core.BloomFilterManager;
import com.example.bloom.factory.BloomFilterLoaderFactory;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RList;
import org.redisson.api.RSet;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.example.bloom.constants.RedisDataSourceType.REDIS_LIST;
import static com.example.bloom.constants.RedisDataSourceType.REDIS_SET;

/**
 * @author 86183
 * @description 管理过滤器的实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RedisBloomFilterManager implements BloomFilterManager {

    private final BloomFilterProperties bloomFilterProperties;
    private final RedissonClient redissonClient;
    private final BloomFilterLoaderFactory strategyFactory;

    private final HashMap<String, RBloomFilter<String>> bloomFilters = new HashMap<>();

    /**
     * 初始化布隆过滤器
     */
    @PostConstruct
    public void init() {
        // 创建布隆过滤器
        for (Map.Entry<String, BloomFilterConfig> entry : bloomFilterProperties.getFilterConfigMap().entrySet()) {
            String filterName = entry.getKey();
            BloomFilterConfig config = entry.getValue();
            createBloomFilter(config.getExpectedInsertions(), config.getFalseProbability(), filterName);
        }

        // TODO 使用ApplicationListener监听记载？到底在应用启动时记载还是启动完成再加载？

        // 异步加载数据
        Thread loaderThread = getThread();
        loaderThread.start();
    }

    /**
     * 获取线程
     *
     * @return 加载数据的线程
     */
    private Thread getThread() {
        Thread loaderThread = new Thread(() -> {
            log.info("Start loading bloom filter data");
            loadData();
            log.info("Finish loading bloom filter data");
        });
        loaderThread.setName("bloom-filter-loader");
        loaderThread.setDaemon(true);
        return loaderThread;
    }

    /**
     * 创建布隆过滤器
     *
     * @param expectedInsertions 预期插入数据量
     * @param falseProbability   误判率
     * @param filterName         过滤器的名称
     */
    private void createBloomFilter(long expectedInsertions, double falseProbability, String filterName) {
        RBloomFilter<String> bloomFilter = redissonClient.getBloomFilter(filterName);
        if (!bloomFilter.isExists()) {
            bloomFilter.tryInit(expectedInsertions, falseProbability);
        }
        bloomFilters.put(filterName, bloomFilter);
    }

    /**
     * 从Redis或者Mysql加载数据到布隆过滤器
     */
    private void loadData() {
        for (Map.Entry<String, RBloomFilter<String>> entry : bloomFilters.entrySet()) {
            String filterName = entry.getKey();

            if (loadDataFromRedis(filterName)) {
                log.info("Load data from redis successfully");
                continue;
            }

            if (loadDataFromDataLoader(filterName)) {
                log.info("Load data from data loader successfully");
                continue;
            }

            log.error("Failed to load data from redis and data loader");
        }
    }

    /**
     * 从Redis中加载数据
     *
     * @param filterName 过滤器的名称
     * @return 是否加载成功
     */
    private boolean loadDataFromRedis(String filterName) {
        RBloomFilter<String> bloomFilter = bloomFilters.get(filterName);
        Map<String, BloomFilterConfig> filterConfigMap = bloomFilterProperties.getFilterConfigMap();
        BloomFilterConfig config = filterConfigMap.get(filterName);

        String redisKey = config.getRedisKey();
        if (redisKey != null && !redisKey.isEmpty()) {
            String redisDataSourceType = config.getRedisDataSourceType();
            return handlerDataFromRedis(redisKey, redisDataSourceType, bloomFilter);
        }
        return false;
    }

    /**
     * 根据不同的数据类型处理从Redis中加载的数据
     *
     * @param redisKey            加载的key
     * @param redisDataSourceType 加载的key的类型
     * @param bloomFilter         加载到哪个布隆过滤器
     * @return 是否加载成功
     */
    private boolean handlerDataFromRedis(String redisKey, String redisDataSourceType, RBloomFilter<String> bloomFilter) {
        switch (redisDataSourceType) {
            case REDIS_SET: {
                RSet<String> set = redissonClient.getSet(redisKey);
                for (String item : set.readAll()) {
                    bloomFilter.add(item);
                }
                break;
            }
            case REDIS_LIST: {
                RList<String> list = redissonClient.getList(redisKey);
                for (String item : list.readAll()) {
                    bloomFilter.add(item);
                }
                break;
            }
            default: {
                log.error("Unknown redis data source type");
                throw new RuntimeException("Unknown redis data source type, check redis-data-source-type property in your configuration");
            }
        }
        return true;
    }

    /**
     * 从data loader中加载数据
     *
     * @param filterName 过滤器的名称
     * @return 是否加载成功
     */
    private boolean loadDataFromDataLoader(String filterName) {
        RBloomFilter<String> bloomFilter = bloomFilters.get(filterName);
        BloomDataLoaderStrategy loader = strategyFactory.getLoader(filterName);
        if (loader != null) {
            List<String> dataList = loader.loadData();
            for (String data : dataList) {
                bloomFilter.add(data);
            }
            return true;
        }
        log.error("No data loader found for filterName: {}", filterName);
        throw new RuntimeException("No data loader found for filterName: " + filterName + ", maybe you should implements BloomDaaLoaderStrategy");
    }

    // TODO 用户配置定时任务，更新布隆过滤器，也使用异步，定时任务的配置也要由用户传入

    @Override
    public boolean mightContain(String filterName, String value) {
        RBloomFilter<String> bloomFilter = bloomFilters.get(filterName);
        return bloomFilter != null && bloomFilter.contains(value);
    }

    @Override
    public void add(String filterName, String value) {
        RBloomFilter<String> bloomFilter = bloomFilters.get(filterName);
        if (bloomFilter != null) {
            bloomFilter.add(value);
        }
    }
}

