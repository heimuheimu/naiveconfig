package com.heimuheimu.naiveconfig.redis;

import com.heimuheimu.naiveconfig.NaiveConfigManager;
import com.heimuheimu.naiveconfig.exception.NaiveConfigException;
import com.heimuheimu.naiveconfig.redis.data.RedisArray;
import com.heimuheimu.naiveconfig.redis.data.RedisBulkString;
import com.heimuheimu.naiveconfig.redis.data.RedisData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.Socket;

/**
 * 一次性 Redis 客户端，每次 Redis 操作都会新建立 Socket 连接，在操作结束后关闭该连接。
 * <p>注意：当前实现是线程安全的</p>
 *
 * @author heimuheimu
 * @ThreadSafe
 */
@SuppressWarnings("WeakerAccess")
public class OneTimeRedisClient {

    private static final Logger LOG = LoggerFactory.getLogger(OneTimeRedisClient.class);

    /**
     * Redis 服务主机地址，由主机名和端口组成，":"符号分割，例如：localhost:6379
     */
    private final String host;

    /**
     * Redis 服务主机名，例如：localhost
     */
    private final String hostname;

    /**
     * Redis 服务端口号
     */
    private final int port;

    /**
     * 构造一个一次性 Redis 客户端
     *
     * @param host Redis 服务主机地址，由主机名和端口组成，":"符号分割，例如：localhost:6379
     * @throws IllegalArgumentException 如果 Redis 服务主机地址不符合规则，将会抛出此异常
     */
    public OneTimeRedisClient(String host) throws IllegalArgumentException {
        this.host = host;
        try {
            String[] hostParts = host.split(":");
            hostname = hostParts[0];
            port = Integer.parseInt(hostParts[1]);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid redis host: `" + host + "`. Valid host example: `localhost:6379`.", e);
        }
    }

    /**
     * 获得 Redis 服务主机地址，由主机名和端口组成，":"符号分割，例如：localhost:6379
     *
     * @return Redis 服务主机地址，由主机名和端口组成，":"符号分割，例如：localhost:6379
     */
    public String getHost() {
        return host;
    }

    /**
     * 从 Redis 中获取 Key 对应的 Java 对象，如果 Key 不存在，将返回 {@code null}。Key 不允许为 {@code null}，
     * 且字节长度不应超过 {@link NaiveConfigManager#MAX_KEY_LENGTH}。
     *
     * @param key Redis key，不允许为 {@code null}，且字节长度不应超过 {@link NaiveConfigManager#MAX_KEY_LENGTH}
     * @return Key 对应的 Java 对象，如果 Key 不存在，将返回 {@code null}
     * @throws NullPointerException 如果 Key 为 {@code null}，将抛出此异常
     * @throws IllegalArgumentException Key 字节长度超过 {@link NaiveConfigManager#MAX_KEY_LENGTH}，将抛出此异常
     * @throws NaiveConfigException 获取过程中如果发生异常，将抛出此异常
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String key) throws NullPointerException, IllegalArgumentException, NaiveConfigException {
        if (key == null) {
            throw new NullPointerException("Key could not be null. Key: `null`. Host: `" + host + "`.");
        }
        Socket socket = null;
        try {
            byte[] keyBytes = getKeyBytes(key);
            RedisData[] getCommandDatas = new RedisData[2];
            getCommandDatas[0] = new RedisBulkString("GET".getBytes(RedisData.UTF8));
            getCommandDatas[1] = new RedisBulkString(keyBytes);
            RedisArray getCommand = new RedisArray(getCommandDatas);
            socket = new Socket(hostname, port);
            OutputStream outputStream = socket.getOutputStream();
            outputStream.write(getCommand.getRespByteArray());
            outputStream.flush();
            InputStream inputStream = socket.getInputStream();
            RedisDataReader reader = new RedisDataReader(inputStream);
            RedisData responseData = reader.read();
            if (responseData.isBulkString()) {
                byte[] valueBytes = responseData.getValueBytes();
                if (valueBytes != null) {
                    return (T) decode(valueBytes);
                } else {
                    return null;
                }
            } else if (responseData.isError()) {
                LOG.error("Get `" + key + "` failed. Redis error message: `" + responseData.getText() + "`. Host: `" + host + "`.");
                throw new NaiveConfigException("Get `" + key + "` failed. Redis error message: `" + responseData.getText() + "`. Host: `" + host + "`.");
            } else {
                //should not happen
                LOG.error("Unrecognized redis response data for `GET` command. Expect data type: `Bulk strings`. Actual: `"
                        + responseData + "`. Key: `" + key + "`. Host: `" + host + "`.");
                throw new NaiveConfigException("Unrecognized redis response data for `GET` command. Expect data type: `Bulk strings`. Actual: `"
                        + responseData + "`. Key: `" + key + "`. Host: `" + host + "`.");
            }
        } catch (Exception e) {
            LOG.error("Unexpected error. Get `" + key + "` failed. Host: `" + host + "`.", e);
            throw new NaiveConfigException("Unexpected error. Get `" + key + "` failed. Host: `" + host + "`.", e);
        } finally {
            if (socket != null) {
                try {
                    socket.close();
                } catch (Exception e) {
                    LOG.error("Close socket failed. Get `" + key + "`. Host: `" + host + "`.", e);
                }
            }
        }
    }

    /**
     * 在 Redis 中设置 Key 对应的 Java 对象，Key 不允许为 {@code null}，且字节长度不应超过 {@link NaiveConfigManager#MAX_KEY_LENGTH}。
     * Value 不允许为 {@code null}，且字节长度不应超过 512 MB。
     *
     * @param key Redis key，不允许为 {@code null}，且字节长度不应超过 {@link NaiveConfigManager#MAX_KEY_LENGTH}
     * @param value Key 对应的 Java 对象，不允许为 {@code null}，且字节长度不应超过 512 MB
     * @throws NullPointerException 如果 Key 为 {@code null}，将抛出此异常
     * @throws NullPointerException 如果 Value 为 {@code null}，将抛出此异常
     * @throws IllegalArgumentException Key 字节长度超过 {@link NaiveConfigManager#MAX_KEY_LENGTH}，将抛出此异常
     * @throws NaiveConfigException 设置过程中如果发生异常，将抛出此异常
     */
    public void set(String key, Object value) throws NullPointerException, IllegalArgumentException, NaiveConfigException {
        if (key == null) {
            throw new NullPointerException("Key could not be null. Key: `null`. Value: `" + value + "`. Host: `" + host + "`.");
        }
        if (value == null) {
            throw new NullPointerException("Value could not be null. Key: `" + key + "`. Value: `null`. Host: `" + host + "`.");
        }
        Socket socket = null;
        try {
            byte[] keyBytes = getKeyBytes(key);
            RedisData[] setCommandDatas = new RedisData[3];
            setCommandDatas[0] = new RedisBulkString("SET".getBytes(RedisData.UTF8));
            setCommandDatas[1] = new RedisBulkString(keyBytes);
            setCommandDatas[2] = new RedisBulkString(encode(value));
            RedisArray setCommand = new RedisArray(setCommandDatas);
            socket = new Socket(hostname, port);
            OutputStream outputStream = socket.getOutputStream();
            outputStream.write(setCommand.getRespByteArray());
            outputStream.flush();
            InputStream inputStream = socket.getInputStream();
            RedisDataReader reader = new RedisDataReader(inputStream);
            RedisData responseData = reader.read();
            if (responseData.isError()) {
                LOG.error("Set `" + key + "` failed. Value: `" + value + "`. Redis error message: `" + responseData.getText() + "`. Host: `" + host + "`.");
                throw new NaiveConfigException("Set `" + key + "` failed. Value: `" + value + "`. Redis error message: `" + responseData.getText() + "`. Host: `" + host + "`.");
            } else if(responseData.isSimpleString()) {
                if( !"OK".equals(responseData.getText()) ) {
                    //should not happen
                    LOG.error("Unrecognized redis response data for `SET` command. Expect data type: `Simple strings`. Expect value: `OK`. Actual: `"
                            + responseData + "`. Host: `" + host + "`. Key: `" + key + "`. Value: `" + value + "`.");
                    throw new NaiveConfigException("Unrecognized redis response data for `SET` command. Expect data type: `Simple strings`. Expect value: `OK`. Actual: `"
                            + responseData + "`. Host: `" + host + "`. Key: `" + key + "`. Value: `" + value + "`.");
                }
            } else {
                //should not happen
                LOG.error("Unrecognized redis response data for `SET` command. Expect data type: `Simple strings`. Expect value: `OK`. Actual: `"
                        + responseData + "`. Host: `" + host + "`. Key: `" + key + "`. Value: `" + value + "`.");
                throw new NaiveConfigException("Unrecognized redis response data for `SET` command. Expect data type: `Simple strings`. Expect value: `OK`. Actual: `"
                        + responseData + "`. Host: `" + host + "`. Key: `" + key + "`. Value: `" + value + "`.");
            }
        } catch (Exception e) {
            LOG.error("Unexpected error. Set `" + key + "` failed. Value: `" + value + "`. Host: `" + host + "`.", e);
            throw new NaiveConfigException("Unexpected error. Set `" + key + "` failed. Value: `" + value + "`. Host: `" + host + "`.", e);
        } finally {
            if (socket != null) {
                try {
                    socket.close();
                } catch (Exception e) {
                    LOG.error("Close socket failed. Set `" + key + "`. Value: `" + value + "`. Host: `" + host + "`.", e);
                }
            }
        }
    }

    /**
     * 从 Redis 中删除指定的 Key，Key 不允许为 {@code null}，且字节长度不应超过 {@link NaiveConfigManager#MAX_KEY_LENGTH}。
     *
     * @param key Redis key，不允许为 {@code null}，且字节长度不应超过 {@link NaiveConfigManager#MAX_KEY_LENGTH}
     * @return 是否删除成功
     * @throws NullPointerException 如果 Key 为 {@code null}，将抛出此异常
     * @throws IllegalArgumentException Key 字节长度超过 {@link NaiveConfigManager#MAX_KEY_LENGTH}，将抛出此异常
     * @throws NaiveConfigException 删除过程中如果发生异常，将抛出此异常
     */
    public boolean delete(String key) throws NullPointerException, IllegalArgumentException, NaiveConfigException {
        if (key == null) {
            throw new NullPointerException("Key could not be null. Key: `null`. Host: `" + host + "`.");
        }
        Socket socket = null;
        try {
            byte[] keyBytes = getKeyBytes(key);
            RedisData[] delCommandDatas = new RedisData[2];
            delCommandDatas[0] = new RedisBulkString("DEL".getBytes(RedisData.UTF8));
            delCommandDatas[1] = new RedisBulkString(keyBytes);
            RedisArray delCommand = new RedisArray(delCommandDatas);
            socket = new Socket(hostname, port);
            OutputStream outputStream = socket.getOutputStream();
            outputStream.write(delCommand.getRespByteArray());
            outputStream.flush();
            InputStream inputStream = socket.getInputStream();
            RedisDataReader reader = new RedisDataReader(inputStream);
            RedisData responseData = reader.read();
            if (responseData.isInteger()) {
                long deletedRows = Long.parseLong(responseData.getText());
                return deletedRows == 1;
            } else if (responseData.isError()) {
                LOG.error("Delete `" + key + "` failed. Redis error message: `" + responseData.getText() + "`. Host: `" + host + "`.");
                throw new NaiveConfigException("Delete `" + key + "` failed. Redis error message: `" + responseData.getText() + "`. Host: `" + host + "`.");
            } else {
                //should not happen
                LOG.error("Unrecognized redis response data for `Delete` command. Expect data type: `Integers`. Actual: `"
                        + responseData + "`. Key: `" + key + "`. Host: `" + host + "`.");
                throw new NaiveConfigException("Unrecognized redis response data for `Delete` command. Expect data type: `Integers`. Actual: `"
                        + responseData + "`. Key: `" + key + "`. Host: `" + host + "`.");
            }
        } catch (Exception e) {
            LOG.error("Unexpected error. Delete `" + key + "` failed. Host: `" + host + "`.", e);
            throw new NaiveConfigException("Unexpected error. Delete `" + key + "` failed. Host: `" + host + "`.", e);
        } finally {
            if (socket != null) {
                try {
                    socket.close();
                } catch (Exception e) {
                    LOG.error("Close socket failed. Delete `" + key + "`. Host: `" + host + "`.", e);
                }
            }
        }
    }

    /**
     * 调用 Redis PUBLISH 命令，在指定 Channel 发布一条消息
     *
     * @param channel 消息发布所在的 Channel，仅允许订阅该 Channel 的客户端接收到当前消息
     * @param message 发布的消息内容
     * @return 接收到该消息的 Redis 客户端数量
     * @throws NullPointerException 如果 Channel 为 {@code null}，将抛出此异常
     * @throws NullPointerException 如果 Message 为 {@code null}，将抛出此异常
     * @throws NaiveConfigException 消息发布过程中如果发生异常，将抛出此异常
     */
    public int publish(String channel, String message) throws NullPointerException, NaiveConfigException {
        if (channel == null) {
            throw new NullPointerException("Channel could not be null. Channel: `null`. Message: `" + message + "`. Host: `" + host + "`.");
        }
        if (message == null) {
            throw new NullPointerException("Channel could not be null. Channel: `" + channel + "`. Message: `null`. Host: `" + host + "`.");
        }
        Socket socket = null;
        try {
            RedisData[] publishCommandDatas = new RedisData[3];
            publishCommandDatas[0] = new RedisBulkString("PUBLISH".getBytes(RedisData.UTF8));
            publishCommandDatas[1] = new RedisBulkString(channel.getBytes(RedisData.UTF8));
            publishCommandDatas[2] = new RedisBulkString(message.getBytes(RedisData.UTF8));
            RedisArray publishCommand = new RedisArray(publishCommandDatas);
            socket = new Socket(hostname, port);
            OutputStream outputStream = socket.getOutputStream();
            outputStream.write(publishCommand.getRespByteArray());
            outputStream.flush();
            InputStream inputStream = socket.getInputStream();
            RedisDataReader reader = new RedisDataReader(inputStream);
            RedisData responseData = reader.read();
            if (responseData.isInteger()) {
                long receivedClients = Long.parseLong(responseData.getText());
                return (int) receivedClients;
            } else if (responseData.isError()) {
                LOG.error("Publish `" + message + "` failed. Channel: `" + channel + "`. Redis error message: `" + responseData.getText() + "`. Host: `" + host + "`.");
                throw new NaiveConfigException("Publish `" + message + "` failed. Channel: `" + channel + "`. Redis error message: `" + responseData.getText() + "`. Host: `" + host + "`.");
            } else {
                //should not happen
                LOG.error("Unrecognized redis response data for `PUBLISH` command. Expect data type: `Integers`. Actual: `"
                        + responseData + "`. Channel: `" + channel + "`. Message: `" + message + "`. Host: `" + host + "`.");
                throw new NaiveConfigException("Unrecognized redis response data for `PUBLISH` command. Expect data type: `Integers`. Actual: `"
                        + responseData + "`. Channel: `" + channel + "`. Message: `" + message + "`. Host: `" + host + "`.");
            }
        } catch (Exception e) {
            LOG.error("Unexpected error. Publish `" + message + "` failed. Channel: `" + channel + "`. Host: `" + host + "`.", e);
            throw new NaiveConfigException("Unexpected error. Publish `" + message + "` failed. Channel: `" + channel + "`. Host: `" + host + "`.", e);
        } finally {
            if (socket != null) {
                try {
                    socket.close();
                } catch (Exception e) {
                    LOG.error("Close socket failed. Publish `" + message + "` at channel `" + channel + "`. Host: `" + host + "`.", e);
                }
            }
        }
    }

    /**
     * 将 Java 对象编码成字节数组后返回
     *
     * @param value 需要进行编码的 Java 对象
     * @return Java 对象编码后的字节数组
     */
    private byte[] encode(Object value) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bos);
        oos.writeObject(value);
        return bos.toByteArray();
    }

    /**
     * 将字节数组解码还原成 Java 对象后返回
     *
     * @param encodedBytes 对象编码后的字节数组
     * @return Java 对象
     */
    private Object decode(byte[] encodedBytes) throws IOException, ClassNotFoundException {
        ByteArrayInputStream valueBis = new ByteArrayInputStream(encodedBytes);
        ObjectInputStream ois = new ObjectInputStream(valueBis);
        return ois.readObject();
    }

    private byte[] getKeyBytes(String key) throws IllegalArgumentException {
        byte[] keyBytes = key.getBytes(RedisData.UTF8);
        if (keyBytes.length > NaiveConfigManager.MAX_KEY_LENGTH) {
            LOG.error("Key is too large. Key length could not greater than " + NaiveConfigManager.MAX_KEY_LENGTH + ". Invalid key: `"
                    + key + "`. Host: `" + host + "`.");
            throw new IllegalArgumentException("Key is too large. Key length could not greater than " + NaiveConfigManager.MAX_KEY_LENGTH
                    + ". Invalid key: `" + key + "`. Host: `" + host + "`.");
        }
        return keyBytes;
    }

}
