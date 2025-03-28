使用：

1、引入jar包

2、配置文件
```
spring:
  data:
    redis:
      host: 192.168.1.2
      port: 6379
      password: "123456"

bloom:
  enabled: true
  filter-config-map:
    # 布隆过滤器的名称
    test-filter:
      # 预期存入数量
      expected-insertions: 1000
      # 误判率
      false-probability: 0.02
      # 存入的过滤器的值先从Redis里面取，则配置从Redis对应的键取，如果不需要配置，则不用写下面两行
      redis-key: ""
      # 这个Redis的数据类型,只支持 set 或 list
      redis-data-source-type:
```
3、编写一个类实现BloomDataLoaderStrategy接口，类上添加注解@BloomFilterLoader("test-filter"),value是布隆过滤器的名称
```
@Component
@BloomFilterLoader("test-filter")
public static class TestDataLoader implements BloomDataLoaderStrategy {
    @Override
    public List<String> loadData() {
        return Arrays.asList("user1", "user2", "user3");
    }
}
```
4、测试
```
    @Autowired(required = false)
    private BloomFilterManager bloomFilterManager;

    @Test
    void contextLoads() {
        System.out.println(bloomFilterManager.mightContain("test-filter", "user1"));
    }
```

