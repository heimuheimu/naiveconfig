/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2017 heimuheimu
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.heimuheimu.naiveconfig.redis;

import com.heimuheimu.naiveconfig.NaiveConfigManager;
import com.heimuheimu.naiveconfig.exception.NaiveConfigException;

/**
 * 基于 Redis 服务实现的 NaiveConfig 配置管理器，提供配置信息获取、设置等管理操作。
 * 在每次配置信息变更后，都会通过 Redis 的 PUBLISH 命令，通知已订阅该 Channel 的 NaiveConfig 客户端，PUBLISH 的消息为变更的配置信息 Key。
 *
 * <p>更多的 PUB/SUB 信息请参考文档：<a href="https://redis.io/topics/pubsub">https://redis.io/topics/pubsub</a></p>
 *
 * <p>更多 Redis 信息请参考：<a href="https://redis.io">https://redis.io</a></p>
 *
 * <p><strong>说明：</strong>{@code RedisNaiveConfigManager} 类是线程安全的，可在多个线程中使用同一个实例。</p>
 *
 * @author heimuheimu
 */
public class RedisNaiveConfigManager implements NaiveConfigManager {

    private final OneTimeRedisClient redisClient;

    private final String channel;

    /**
     * 构造一个 NaiveConfig 配置管理器。
     *
     * @param host Redis 服务主机地址，由主机名和端口组成，":"符号分割，例如：localhost:6379
     * @param channel 当配置信息变更后，会通过 PUBLISH 命令在该 Channel 发布变更的配置信息 Key，所有订阅该 Channel 的 NaiveConfig 客户端将会接到通知
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
}
