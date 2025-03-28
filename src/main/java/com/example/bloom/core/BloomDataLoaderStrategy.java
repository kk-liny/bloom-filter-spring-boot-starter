package com.example.bloom.core;

import java.util.List;

/**
 * @author 86183
 * @description 数据加载器的策略接口
 */
public interface BloomDataLoaderStrategy {
    List<String> loadData();
}
