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

import com.heimuheimu.naiveconfig.NaiveConfigClient;
import com.heimuheimu.naiveconfig.NaiveConfigClientListener;
import com.heimuheimu.naiveconfig.constant.BeanStatusEnum;
import com.heimuheimu.naiveconfig.exception.NaiveConfigException;
import com.heimuheimu.naiveconfig.redis.subscribe.RedisSubscribeClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;

/**
 * 基于 Redis 服务实现的 NaiveConfig 客户端，配置信息变更监听通过 Redis PUB/SUB 命令实现。
 *
 * <p>更多的 PUB/SUB 信息请参考文档：<a href="https://redis.io/topics/pubsub">https://redis.io/topics/pubsub</a></p>
 *
 * <p>更多 Redis 信息请参考：<a href="https://redis.io">https://redis.io</a></p>
 *
 * <p><strong>说明：</strong>{@code RedisNaiveConfigClient} 类是线程安全的，可在多个线程中使用同一个实例。</p>
 *
 * @author heimuheimu
 */
public class RedisNaiveConfigClient implements NaiveConfigClient, Closeable {

    private static final Logger LOG = LoggerFactory.getLogger(RedisNaiveConfigClient.class);

    private final OneTimeRedisClient redisClient;

    private final String host;

    private final String channel;

    private final int pingPeriod;

    private final NaiveConfigClientListener listener;

    private volatile RedisSubscribeClient redisSubscribeClient;

    private volatile BeanStatusEnum state = BeanStatusEnum.UNINITIALIZED;

    /**
     * 私有锁
     */
    private final Object lock = new Object();

    /**
     * Redis 订阅客户端恢复任务使用的私有锁
     */
    private final Object rescueTaskLock = new Object();

    /**
     * 构造一个基于 Redis 服务实现的 NaiveConfig 客户端，默认 Redis 操作超时时间为 30 秒。
     *
     * @param host Redis 服务主机地址，由主机名和端口组成，":"符号分割，例如：localhost:6379
     * @param channel  当前 Redis 订阅客户端订阅的 Channel 信息，不允许为 {@code null} 或空字符串
     * @param pingPeriod PING 命令发送时间间隔，单位：秒。用于心跳检测。如果该值小于等于 0，则不进行心跳检测
     * @param listener NaiveConfig 客户端事件监听器，不允许为 {@code null}
     * @throws IllegalArgumentException 如果 Redis 服务主机地址不符合规则，将会抛出此异常
     * @throws IllegalArgumentException 如果 Channel 为 {@code null} 或空字符串，将会抛出此异常
     * @throws NullPointerException 如果 listener 为 {@code null}，将会抛出此异常
     */
    public RedisNaiveConfigClient(String host, String channel, int pingPeriod, NaiveConfigClientListener listener) throws IllegalArgumentException {
        this(host, channel, pingPeriod, listener, 30000);
    }

    /**
     * 构造一个基于 Redis 服务实现的 NaiveConfig 客户端。
     *
     * @param host Redis 服务主机地址，由主机名和端口组成，":"符号分割，例如：localhost:6379
     * @param channel  当前 Redis 订阅客户端订阅的 Channel 信息，不允许为 {@code null} 或空字符串
     * @param pingPeriod PING 命令发送时间间隔，单位：秒。用于心跳检测。如果该值小于等于 0，则不进行心跳检测
     * @param listener NaiveConfig 客户端事件监听器，不允许为 {@code null}
     * @param timeout Redis 操作超时时间，单位：毫秒，不允许小于等于 0
     * @throws IllegalArgumentException 如果 Redis 服务主机地址不符合规则，将会抛出此异常
     * @throws IllegalArgumentException 如果 Channel 为 {@code null} 或空字符串，将会抛出此异常
     * @throws NullPointerException 如果 listener 为 {@code null}，将会抛出此异常
     * @throws IllegalArgumentException 如果 Redis 操作超时时间小于等于 0，将会抛出此异常
     */
    public RedisNaiveConfigClient(String host, String channel, int pingPeriod, NaiveConfigClientListener listener, int timeout) throws IllegalArgumentException {
        this.host = host;
        this.channel = channel;
        this.pingPeriod = pingPeriod;
        this.listener = listener;
        this.redisClient = new OneTimeRedisClient(host, timeout);
    }

    @Override
    public String getHost() {
        return host;
    }

    @Override
    public <T> T get(String key) throws NullPointerException, IllegalArgumentException, NaiveConfigException {
        return redisClient.get(key);
    }

    /**
     * 执行 NaiveConfig 客户端初始化操作。
     *
     * @throws NaiveConfigException 如果初始化过程中发生错误，将会抛出此异常
     */
    public void init() throws NaiveConfigException {
        synchronized (lock) {
            if (state == BeanStatusEnum.UNINITIALIZED) {
                try {
                    this.redisSubscribeClient = createRedisSubscribeClient();
                    state = BeanStatusEnum.NORMAL;
                    try {
                        listener.onInitialized(this);
                    } catch (Exception e) {
                        LOG.error("Call NaiveConfigClientListener#onInitialized() failed. Host: `" + host + "`. Channel: `"
                                + channel + "`. Ping period: `" + pingPeriod + "`.", e);
                    }
                } catch (Exception e) {
                    LOG.error("Create RedisNaiveConfigClient failed. Host: `" + host + ". Channel: `" + channel
                            + "`. Ping period: `" + pingPeriod + "`.", e);
                    throw new NaiveConfigException("Create RedisNaiveConfigClient failed. Host: `" + host + ". Channel: `" + channel
                            + "`. Ping period: `" + pingPeriod + "`.", e);
                }
            }
        }
    }

    /**
     * 执行 NaiveConfig 客户端关闭操作。
     *
     * <p>注意：该方法不会触发 {@link NaiveConfigClientListener#onClosed(NaiveConfigClient)} 事件。</p>
     */
    @Override
    public void close() {
        synchronized (lock) {
            if (state != BeanStatusEnum.CLOSED) {
                state = BeanStatusEnum.CLOSED;
                if (redisSubscribeClient != null) {
                    redisSubscribeClient.close(false);
                }
            }
        }
    }

    private RedisSubscribeClient createRedisSubscribeClient() throws IllegalArgumentException, NaiveConfigException {
        RedisSubscribeClient client = new RedisSubscribeClient(host, channel, pingPeriod) {

            @Override
            protected void onMessageReceived(String message) {
                try {
                    listener.onChanged(RedisNaiveConfigClient.this, message);
                } catch (Exception e) {
                    LOG.error("Call NaiveConfigClientListener#onChanged() failed. Host: `" + host + "`. Channel: `"
                            + channel + "`. Ping period: `" + pingPeriod + "`.", e);
                }
            }

            @Override
            protected void onClosed() {
                startRescueTask();
                try {
                    listener.onClosed(RedisNaiveConfigClient.this);
                } catch (Exception e) {
                    LOG.error("Call NaiveConfigClientListener#onClosed() failed. Host: `" + host + "`. Channel: `"
                            + channel + "`. Ping period: `" + pingPeriod + "`.", e);
                }
            }

        };
        client.init();
        return client;
    }

    private void startRescueTask() {
        if (state == BeanStatusEnum.NORMAL) {
            Thread rescueThread = new Thread() {

                @Override
                public void run() {
                    synchronized (rescueTaskLock) {
                        long startTime = System.currentTimeMillis();
                        LOG.info("RedisSubscribeClient rescue task has been started. Host: `{}`. Channel: `{}`. Ping period: `{}`.",
                                host, channel, pingPeriod);
                        RedisSubscribeClient client = null;
                        while (client == null) {
                            try {
                                client = createRedisSubscribeClient();
                                redisSubscribeClient = client;
                                LOG.info("Rescue RedisSubscribeClient success. Cost: {}ms. Host: `{}`. Channel: `{}`. Ping period: `{}`.",
                                        (System.currentTimeMillis() - startTime), host, channel, pingPeriod);
                                try {
                                    listener.onRecovered(RedisNaiveConfigClient.this);
                                } catch (Exception e) {
                                    LOG.error("Call NaiveConfigClientListener#onRecovered() failed. Host: `" + host + "`. Channel: `"
                                            + channel + "`. Ping period: `" + pingPeriod + "`.", e);
                                }
                            } catch (Exception e) {
                                LOG.error("Rescue RedisSubscribeClient failed. Cost: " + (System.currentTimeMillis() - startTime) +
                                        "ms. Host: `" + host + "`. Channel: `" + channel + "`. Ping period: `" + pingPeriod + "`.", e);
                                try {
                                    Thread.sleep(5000);
                                } catch (InterruptedException e1) {
                                    //ignore exception
                                }
                            }
                        }
                    }
                }
            };
            rescueThread.setName("RedisNaiveConfigClient-rescue-task");
            rescueThread.setDaemon(true);
            rescueThread.start();
        }
    }

}
