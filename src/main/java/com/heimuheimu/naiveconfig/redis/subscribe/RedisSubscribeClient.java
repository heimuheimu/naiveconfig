package com.heimuheimu.naiveconfig.redis.subscribe;

import com.heimuheimu.naiveconfig.constant.BeanStatusEnum;
import com.heimuheimu.naiveconfig.exception.NaiveConfigException;
import com.heimuheimu.naiveconfig.redis.RedisDataReader;
import com.heimuheimu.naiveconfig.redis.data.RedisArray;
import com.heimuheimu.naiveconfig.redis.data.RedisBulkString;
import com.heimuheimu.naiveconfig.redis.data.RedisData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Redis 订阅客户端，自动接收指定 Channel 的消息
 * <p>更多的 PUB/SUB 信息请参考文档：<a href="https://redis.io/topics/pubsub">https://redis.io/topics/pubsub</a></p>
 * <p>更多 Redis 信息请参考：<a href="https://redis.io">https://redis.io</a></p>
 * <p>当前是实现是线程安全的。</p>
 *
 * @author heimuheimu
 * @ThreadSafe
 */
public abstract class RedisSubscribeClient implements Closeable {

    private static final Logger LOG = LoggerFactory.getLogger(RedisSubscribeClient.class);

    /**
     * Redis 服务主机地址，由主机名和端口组成，":"符号分割，例如：localhost:6379
     */
    private final String host;

    /**
     * 当前 Redis 订阅客户端订阅的 Channel 信息
     */
    private final String channel;

    /**
     * PING 命令发送时间间隔，单位：秒。用于心跳检测。
     */
    private final int pingPeriod;

    /**
     * 与 Redis 服务建立的 Socket 连接
     */
    private final Socket socket;

    /**
     * Redis 数据读取器
     */
    private final RedisDataReader reader;

    /**
     * 当前实例所处状态
     */
    private volatile BeanStatusEnum state = BeanStatusEnum.UNINITIALIZED;

    /**
     * 心跳检测任务执行器
     */
    private ScheduledExecutorService pingExecutorService = null;

    /**
     * 未收到返回值的 PING 命令数量
     */
    private final AtomicInteger unconfirmedPingCount = new AtomicInteger();

    /**
     * 当前实例使用的私有锁
     */
    private final Object lock = new Object();

    /**
     * 构造一个 Redis 订阅客户端，心跳检测时间为 30 秒
     *
     * @param host Redis 服务主机地址，由主机名和端口组成，":"符号分割，例如：localhost:6379
     * @param channel 当前 Redis 订阅客户端订阅的 Channel 信息
     * @throws IllegalArgumentException 如果 Redis 服务主机地址不符合规则，将会抛出此异常
     * @throws NaiveConfigException 如果 与 Redis 服务建立的 Socket 连接过程中发生错误，将会抛出此异常
     */
    public RedisSubscribeClient(String host, String channel) throws IllegalArgumentException, NaiveConfigException {
        this(host, channel, 30);
    }

    /**
     * 构造一个 Redis 订阅客户端
     * <p>注意：实例创建完成后，需调用 {@link #init()} 方法进行初始化操作后才能使用</p>
     *
     * @param host Redis 服务主机地址，由主机名和端口组成，":"符号分割，例如：localhost:6379
     * @param channel 当前 Redis 订阅客户端订阅的 Channel 信息，不允许为 {@code null} 或空字符串
     * @param pingPeriod PING 命令发送时间间隔，单位：秒。用于心跳检测。如果该值小于等于 0，则不进行心跳检测
     * @throws IllegalArgumentException 如果 Redis 服务主机地址不符合规则，将会抛出此异常
     * @throws IllegalArgumentException 如果 Channel 为 {@code null} 或空字符串，将会抛出此异常
     * @throws NaiveConfigException 如果与 Redis 服务建立的 Socket 连接过程中发生错误，将会抛出此异常
     */
    public RedisSubscribeClient(String host, String channel, int pingPeriod) throws IllegalArgumentException, NaiveConfigException {
        if (channel == null || channel.isEmpty()) {
            LOG.error("Create RedisSubscribeClient failed. Channel could not be null or empty. Host: `" + host + "`. Channel: `"
                    + channel + "`. Ping period: `" + pingPeriod + "`.");
            throw new IllegalArgumentException("Create RedisSubscribeClient failed. Channel could not be null or empty. Host: `" + host + "`. Channel: `"
                    + channel + "`. Ping period: `" + pingPeriod + "`.");
        }
        this.host = host;
        this.channel = channel;
        this.pingPeriod = pingPeriod;
        String hostname;
        int port;
        try {
            String[] hostParts = host.split(":");
            hostname = hostParts[0];
            port = Integer.parseInt(hostParts[1]);
        } catch (Exception e) {
            LOG.error("Create RedisSubscribeClient failed. Invalid redis host: `" + host + "`. Valid host example: `localhost:6379`. Channel: `"
                    + channel + "`. Ping period: `" + pingPeriod + "`.", e);
            throw new IllegalArgumentException("Create RedisSubscribeClient failed. Invalid redis host: `" + host
                    + "`. Valid host example: `localhost:6379`. Channel: `" + channel + "`. Ping period: `" + pingPeriod + "`.", e);
        }
        try {
            this.socket = new Socket(hostname, port);
            this.reader = new RedisDataReader(socket.getInputStream());
        } catch (Exception e) {
            LOG.error("Create RedisSubscribeClient failed. Host: `" + host + "`. Channel: `" + channel
                    + "`. Ping period: `" + pingPeriod + "`.", e);
            throw new NaiveConfigException("Create RedisSubscribeClient failed. Host: `" + host + "`. Channel: `" + channel
                    + "`. Ping period: `" + pingPeriod + "`.", e);
        }
    }

    /**
     * 执行 Redis 订阅客户端初始化操作：
     * <ol>
     *   <li>调用 SUBSCRIBE 命令</li>
     *   <li>启动 IO 线程，用于接收在订阅 Channel 发布的消息</li>
     *   <li>如果心跳检测时间设置大于 0，则启动心跳检测线程</li>
     * </ol>
     *
     * @throws NaiveConfigException 如果在调用 Subscribe 命令过程中发生错误，将会抛出此异常
     */
    public void init() throws NaiveConfigException {
        synchronized (lock) {
            if (state == BeanStatusEnum.UNINITIALIZED) {
                try {
                    long startTime = System.currentTimeMillis();
                    //调用 SUBSCRIBE 命令
                    RedisData[] subscribeCommandDatas = new RedisData[2];
                    subscribeCommandDatas[0] = new RedisBulkString("SUBSCRIBE".getBytes(RedisData.UTF8));
                    subscribeCommandDatas[1] = new RedisBulkString(channel.getBytes(RedisData.UTF8));
                    RedisArray subscribeCommand = new RedisArray(subscribeCommandDatas);
                    sendCommand(subscribeCommand);
                    RedisData responseData = reader.read();
                    if (!responseData.isArray() || responseData.size() != 3
                            || !"subscribe".equals(responseData.get(0).getText().toLowerCase())) {
                        LOG.error("Initialize RedisSubscribeClient failed. Unrecognized redis response data for `Subscribe` command. " +
                                "Expect data type: `Arrays with three elements.`. Expect value: `subscribe ${channel} 1`. Actual: `"
                                + responseData + "`. Host: `" + host + "`. Channel: `" + channel + "`. Ping period: `" + pingPeriod + "`.");
                        throw new NaiveConfigException("Initialize RedisSubscribeClient failed. Unrecognized redis response data for `Subscribe` command. " +
                                "Expect data type: `Arrays with three elements.`. Expect value: `subscribe ${channel} 1`. Actual: `"
                                + responseData + "`. Host: `" + host + "`. Channel: `" + channel + "`. Ping period: `" + pingPeriod + "`.");
                    }
                    //启动 IO 线程，用于接收在订阅 Channel 发布的消息
                    SubscribeIoThread t = new SubscribeIoThread();
                    t.setName("NaiveConfig-Redis-Subscriber-Thread]");
                    t.start();
                    //判断是否需要启动心跳检测线程
                    if (pingPeriod > 0) {
                        pingExecutorService = Executors.newSingleThreadScheduledExecutor();
                        pingExecutorService.scheduleAtFixedRate(new Runnable() {

                            @Override
                            public void run() {
                                if (unconfirmedPingCount.get() > 0) {
                                    LOG.error("Previous `PING` command has not confirmed. RedisSubscribeClient should be closed. Host: `"
                                            + host + "`. Channel: " + channel + "`. Ping period: `" + pingPeriod + "`.");
                                    close();
                                } else {
                                    try {
                                        unconfirmedPingCount.incrementAndGet();
                                        RedisData[] pingCommandDatas = new RedisData[1];
                                        pingCommandDatas[0] = new RedisBulkString("PING".getBytes(RedisData.UTF8));
                                        sendCommand(new RedisArray(pingCommandDatas));
                                        LOG.debug("Send `PING` success. Host: `{}`. Channel: `{}`. Ping period: `{}`.", host, channel, pingPeriod);
                                    } catch (Exception e) {
                                        LOG.error("Send `PING` command failed. RedisSubscribeClient should be closed. Host: `" + host + "`. Channel: `"
                                                + channel + "`. Ping period: `" + pingPeriod + "`.", e);
                                        close();
                                    }
                                }
                            }

                        }, pingPeriod, pingPeriod, TimeUnit.SECONDS);
                    }
                    state = BeanStatusEnum.NORMAL;
                    LOG.info("RedisSubscribeClient been initialized. Cost: {}ms. Host: `{}`. Channel: `{}`. Ping period: `{}`.",
                            (System.currentTimeMillis() - startTime), host, channel, pingPeriod);
                } catch (NaiveConfigException e) {
                    close(false);
                    throw e;
                } catch (Exception e) {
                    close(false);
                    LOG.error("Initialize RedisSubscribeClient failed. Host: `" + host + "`. Channel: `" + channel + "`. Ping period: `"
                            + pingPeriod + "`.", e);
                    throw new NaiveConfigException("Initialize RedisSubscribeClient failed. Host: `" + host + "`. Channel: `" + channel
                            + "`. Ping period: `" + pingPeriod + "`.", e);
                }
            }
        }
    }

    @Override
    public void close() {
        close(true);
    }

    public void close(boolean triggerOnClosedEvent) {
        synchronized (lock) {
            if (state != BeanStatusEnum.CLOSED) {
                long startTime = System.currentTimeMillis();
                state = BeanStatusEnum.CLOSED;
                try {
                    //关闭 Socket 连接
                    socket.close();
                    //关闭心跳检测线程
                    if (pingExecutorService != null) {
                        pingExecutorService.shutdown();
                    }
                } catch (Exception e) {
                    LOG.error("Close RedisSubscribeClient failed. Unexpected error. Host: `" + host + "`. Channel: `" + channel + "`.", e);
                }
                LOG.info("RedisSubscribeClient has been closed. Cost: {}ms. Host: `{}`. Channel: `{}`.", (System.currentTimeMillis() - startTime),
                        host, channel);
                if (triggerOnClosedEvent) {
                    try {
                        onClosed();
                    } catch (Exception e) {
                        LOG.error("Call RedisSubscribeClient#onClosed() failed. Host: `" + host + "`. Channel: `" + channel + "`.", e);
                    }
                }
            }
        }
    }

    public boolean isActive() {
        return state == BeanStatusEnum.NORMAL;
    }

    protected abstract void onMessageReceived(String message);

    protected abstract void onClosed();

    private void sendCommand(RedisArray command) throws IOException {
        synchronized (lock) {
            OutputStream outputStream = socket.getOutputStream();
            outputStream.write(command.getRespByteArray());
            outputStream.flush();
        }
    }

    private class SubscribeIoThread extends Thread {

        @Override
        public void run() {
            try {
                RedisData responseData;
                while ((responseData = reader.read()) != null) {
                    if (responseData.isArray() && responseData.size() == 3) {
                        if ("message".equals(responseData.get(0).getText().toLowerCase())) {
                            String message = responseData.get(2).getText();
                            try {
                                onMessageReceived(message);
                            } catch (Exception e) {
                                LOG.error("Call RedisSubscribeClient#onMessageReceived() failed. Message: `" + message + "`. Host: `" + host + "`. Channel: `" + channel + "`.", e );
                            }
                        } else {
                            LOG.warn("Unrecognized redis response data for `Subscribe` command. Expect data type: `Arrays with three elements.`. " +
                                            "Expect value: `message ${channel} ${message}`. Actual: `{}`. Host: `{}`. Channel: `{}`.", responseData,
                                    host, channel);
                        }
                    } else if (responseData.isArray() && responseData.size() == 2 && "pong".equals(responseData.get(0).getText().toLowerCase())) {
                        unconfirmedPingCount.decrementAndGet();
                        LOG.debug("PONG. Host: `{}`. Channel: `{}`.", host, channel);
                    } else if (responseData.isError()) {
                        LOG.error("Redis error: `{}`. Host: `{}`. Channel: `{}`.", responseData.getText(), host, channel);
                    } else {
                        LOG.warn("Unrecognized redis response data for `Subscribe` command. Expect data type: `Arrays with three elements.`. " +
                                        "Expect value: `message ${channel} ${message}`. Actual: `{}`. Host: `{}`. Channel: `{}`.", responseData,
                                host, channel);
                    }
                }
                LOG.info("End of the input stream has been reached. Host: `{}`. Channel: `{}`.", host, channel);
                close();
            } catch(SocketException e) {
                // 防止 socket 在外部关闭，再调用一次close方法
                close();
            } catch (Exception e) {
                LOG.error("RedisSubscribeClient need to be closed due to: `" + e.getMessage() + "`. Host: `" + host + "`. Channel: `" + channel + "`.", e);
                close();
            }
        }
    }

}
