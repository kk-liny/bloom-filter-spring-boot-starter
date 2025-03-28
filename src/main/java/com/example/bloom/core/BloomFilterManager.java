package com.example.bloom.core;

/**
 * @author 86183
 * @description 管理全部过滤器的接口
 */
public interface BloomFilterManager {

    /**
     * 判断数据是否存在于布隆过滤器中
     * @param filterName 过滤器的名字
     * @param value 目标数据
     * @return 存在返回true 否则返回false
     */
    boolean mightContain(String filterName, String value);

    /**
     * 向布隆过滤器中添加数据
     * @param filterName 过滤器的名字
     * @param value 目标数据
     */
    void add(String filterName, String value);
}
