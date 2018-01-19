# NaiveConfig: 基于 Redis 实现的配置中心，在集群中实时同步配置信息变更。

## 使用要求
* JDK 版本：1.8+ 
* 依赖类库：
  * [slf4j-log4j12 1.7.5+](https://mvnrepository.com/artifact/org.slf4j/slf4j-log4j12)
  * [naivemonitor 1.0+](https://github.com/heimuheimu/naivemonitor)

## Maven 配置
```xml
    <dependency>
        <groupId>com.heimuheimu</groupId>
        <artifactId>naiveconfig</artifactId>
        <version>1.0</version>
    </dependency>
```
## Log4J 配置
```
log4j.logger.com.heimuheimu.naiveconfig=INFO, NAIVECONFIG_LOGGER
log4j.additivity.com.heimuheimu.naiveconfig=false
log4j.appender.NAIVECONFIG_LOGGER=org.apache.log4j.DailyRollingFileAppender
log4j.appender.NAIVECONFIG_LOGGER.file=${log.output.directory}/naiveconfig/naiveconfig.log
log4j.appender.NAIVECONFIG_LOGGER.encoding=UTF-8
log4j.appender.NAIVECONFIG_LOGGER.DatePattern=_yyyy-MM-dd
log4j.appender.NAIVECONFIG_LOGGER.layout=org.apache.log4j.PatternLayout
log4j.appender.NAIVECONFIG_LOGGER.layout.ConversionPattern=%d{ISO8601} %-5p [%F:%L] : %m%n

# 配置变更信息日志
log4j.logger.com.heimuheimu.naiveconfig.listener=INFO, NAIVECONFIG_CONFIG_SYNC_LOGGER
log4j.additivity.com.heimuheimu.naiveconfig.listener=false
log4j.appender.NAIVECONFIG_CONFIG_SYNC_LOGGER=org.apache.log4j.DailyRollingFileAppender
log4j.appender.NAIVECONFIG_CONFIG_SYNC_LOGGER.file=${log.output.directory}/naiveconfig/config_sync.log
log4j.appender.NAIVECONFIG_CONFIG_SYNC_LOGGER.encoding=UTF-8
log4j.appender.NAIVECONFIG_CONFIG_SYNC_LOGGER.DatePattern=_yyyy-MM-dd
log4j.appender.NAIVECONFIG_CONFIG_SYNC_LOGGER.layout=org.apache.log4j.PatternLayout
log4j.appender.NAIVECONFIG_CONFIG_SYNC_LOGGER.layout.ConversionPattern=%d{ISO8601} %-5p [%F:%L] : %m%n
```

### Spring 配置
```xml
    <!-- NaiveConfig 配置管理器配置，用于变更配置信息 -->
    <bean id="naiveConfigManager" class="com.heimuheimu.naiveconfig.redis.RedisNaiveConfigManager">
        <constructor-arg index="0" value="127.0.0.1:6379" /> <!-- Redis 服务地址 -->
        <constructor-arg index="1" value="config_sync_channel" /> <!-- 用于发布/订阅的 channel 名称 -->
    </bean>
    
    <!-- NaiveConfig 客户端监听器配置，用于同步配置信息变更以及在配置信息同步不可用时进行实时报警 -->
    <bean id="noticeableConfigClientListener" class="com.heimuheimu.naiveconfig.listener.NoticeableConfigClientListener">
        <constructor-arg index="0">
            <util:list>
                <ref bean="ChatKeywordServiceImpl" /> <!-- 配置信息同步处理器，请参考下面的示例代码实现-->
            </util:list>
        </constructor-arg>
        <constructor-arg index="1" value="your-project-name" /> <!-- 当前项目名称 -->
        <constructor-arg index="2" ref="notifierList"/> <!-- 报警器列表，报警器的信息可查看 naivemonitor 项目 -->
    </bean>
    
    <bean id="naiveConfigClient" class="com.heimuheimu.naiveconfig.redis.RedisNaiveConfigClient"
              init-method="init" destroy-method="close">
        <constructor-arg index="0" value="127.0.0.1:6379" /> <!-- Redis 服务地址，与配置管理保持一致 -->
        <constructor-arg index="1" value="config_sync_channel" /> <!-- 用于发布/订阅的 channel 名称，与配置管理保持一致 -->
        <constructor-arg index="2" value="30" /> <!-- Redis 订阅客户端心跳检测周期，单位：秒，建议使用 30 秒-->
        <constructor-arg index="3" ref="noticeableConfigClientListener" /> <!-- NaiveConfig 客户端监听器 -->
    </bean>
```

### 示例代码

场景：聊天关键词变更同步（注意：示例代码仅为说明如何使用 NaiveConfig 进行集群内的配置信息变更同步）。

```java
    @Service
    public class ChatKeywordServiceImpl implements ChatKeywordService, ConfigSyncHandler<CopyOnWriteArrayList<String>> {
        
        // 聊天关键词列表配置信息 Key
        private static final String CHAT_KEYWORDS_CONFIG_KEY = "chat_keywords_config_key";
    
        // 聊天关键词列表，将所有关键词存储在本地内存中
        private volatile CopyOnWriteArrayList<String> chatKeywordList = new CopyOnWriteArrayList<>();
        
        @Autowired
        private ChatKeywordDao chatKeywordDao;
        
        @Override
        public boolean match(String content) { // 判断聊天内容中是否含有关键词
            for (String keyword : chatKeywordList) {
                if (content.contains(keyword)) {
                    return true;
                }
            }
            return false;
        }
        
        @Override
        public void add(String keyword) { // 新增一个聊天关键词
            chatKeywordDao.add(keyword); //调用 Dao 接口新增该关键词
            
            List<String> chatKeywordList = new CopyOnWriteArrayList<>(chatKeywordDao.getAll()); //获取所有的聊天关键词列表
            
            naiveConfigManager.set(CHAT_KEYWORDS_CONFIG_KEY, chatKeywordList); //修改聊天关键词配置信息，并进行变更通知
        }
        
        @Override
        public String getKey() { // 获得配置信息同步处理器监听的 Key
            return CHAT_KEYWORDS_CONFIG_KEY;
        }
    
        @Override
        public void sync(CopyOnWriteArrayList<String> chatKeywordList) { //将变更后的聊天关键词列表存储在本地内存中
            if (chatKeywordList != null) {
                this.chatKeywordList = chatKeywordList;
            }
        }
    }
```

## 更多信息
* [Redis 官网](https://redis.io)
* [NaiveMonitor 项目主页](https://github.com/heimuheimu/naivemonitor)
* [NaiveConfig v1.0 API Doc](https://heimuheimu.github.io/naiveconfig/api/v1.0/)
* [NaiveConfig v1.0 源码下载](https://heimuheimu.github.io/naiveconfig/download/naiveconfig-1.0-sources.jar)
* [NaiveConfig v1.0 Jar包下载](https://heimuheimu.github.io/naiveconfig/download/naiveconfig-1.0.jar)