package com.heimuheimu.naiveconfig.redis;

import com.heimuheimu.naiveconfig.NaiveConfigManager;
import com.heimuheimu.naiveconfig.exception.NaiveConfigException;

/**
 * 基于 Redis 服务实现的 NaiveConfig 配置管理器，提供配置信息获取、设置、删除等管理操作。
 * 在每次配置信息变更后，都会通过 Redis 的 PUBLISH 命令，通知已订阅该 Channel 的 NaiveConfig 客户端，
 * PUBLISH 的消息为变更的配置信息 Key。
 * <p></p>更多的 PUB/SUB 信息请参考文档：<a href="https://redis.io/topics/pubsub">https://redis.io/topics/pubsub</a></p>
 * <p>更多 Redis 信息请参考：<a href="https://redis.io">https://redis.io</a></p>
 * <p>当前是实现是线程安全的。</p>
 *
 * @author heimuheimu
 * @ThreadSafe
 */
public class RedisNaiveConfigManager implements NaiveConfigManager {

    private final OneTimeRedisClient redisClient;

    private final String channel;

    /**
     * 构造一个 NaiveConfig 配置管理器
     *
     * @param host Redis 服务主机地址，由主机名和端口组成，":"符号分割，例如：localhost:6379
     * @param channel 当配置信息变更后，会通过 PUBLISH 命令在该 Channel 发布变更的配置信息 Key，所有订阅 该 Channel 的 NaiveConfig 客户端将会接到通知
     * @throws NullPointerException 如果 Channel 为 {@code null}，将会抛出此异常
     * @throws IllegalArgumentException 如果 Redis 服务主机地址不符合规则，将会抛出此异常
     */
    public RedisNaiveConfigManager(String host, String channel) throws NullPointerException, IllegalArgumentException {
        if (channel == null) {
            throw new NullPointerException("Channel could not be null. Host: `" + host + "`.");
        }
        this.channel = channel;
        this.redisClient = new OneTimeRedisClient(host);
    }

    @Override
    public String getHost() {
        return redisClient.getHost();
    }

    @Override
    public <T> T get(String key) throws NullPointerException, IllegalArgumentException, NaiveConfigException {
        return redisClient.get(key);
    }

    @Override
    public int set(String key, Object value) throws NullPointerException, IllegalArgumentException, NaiveConfigException {
        if (value != null) {
            redisClient.set(key, value);
        }
        return redisClient.publish(channel, key);
    }

    @Override
    public int delete(String key) throws NullPointerException, IllegalArgumentException, NaiveConfigException {
        if (redisClient.delete(key)) {
            return redisClient.publish(channel, key);
        } else { //如果 Key 已经不存在，不进行通知
            return 0;
        }

    }
}
