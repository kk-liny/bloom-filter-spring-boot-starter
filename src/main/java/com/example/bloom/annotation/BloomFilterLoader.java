package com.example.bloom.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author 86183
 * @description 标注数据加载器的注解；数据加载器：由用户实现数据加载策略接口的类
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface BloomFilterLoader {
    /**
     * 数据加载器的名称
     */
    String value() default "";
}
