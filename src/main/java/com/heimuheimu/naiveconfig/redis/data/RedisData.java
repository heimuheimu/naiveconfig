package com.heimuheimu.naiveconfig.redis.data;

import java.nio.charset.Charset;

/**
 * Redis 客户端与服务端进行通信的数据载体，数据格式为 RESP  (Redis Serialization Protocol).
 * 关于该格式定义的更多信息请参考文档：<a href="https://redis.io/topics/protocol">https://redis.io/topics/protocol</a>
 *
 * @author heimuheimu
 */
public abstract class RedisData {

    /**
     * UTF-8 编码
     */
    public static final Charset UTF8 = Charset.forName("utf-8");

    /**
     * CR 控制符字节，`\r`
     */
    public static final byte CR = '\r';

    /**
     * LF 控制符字节，`\n`
     */
    public static final byte LF = '\n';

    /**
     * 是否为 Simple Strings 类型，该数据类型的第一个字节为 "+"
     *
     * @return 是否为 Simple Strings 类型
     */
    public boolean isSimpleString() {
        return false;
    }

    /**
     * 是否为 Errors 类型，该数据类型的第一个字节为 "-"
     *
     * @return 是否为 Errors 类型
     */
    public boolean isError() {
        return false;
    }

    /**
     * 是否为 Integers 类型，该数据类型的第一个字节为 ":"
     *
     * @return 是否为 Integers 类型
     */
    public boolean isInteger() {
        return false;
    }

    /**
     * 是否为 Bulk Strings 类型，该数据类型的第一个字节为 "$"
     *
     * @return 是否为 Bulk Strings 类型
     */
    public boolean isBulkString() {
        return false;
    }

    /**
     * 是否为 Arrays 类型，该数据类型的第一个字节为 "*"
     *
     * @return 是否为 Arrays 类型
     */
    public boolean isArray() {
        return false;
    }

    /**
     * 获得当前数据类型内容对应的字节数组，不包含第一个数据类型字节以及结尾 CR、LF 符，
     * 如果当前数据类型为 Bulk Strings，有可能返回 {@code null}。
     * <p>Arrays 类型数据不允许调用此方法，如果调用，将会抛出 {@link UnsupportedOperationException} 异常。</p>
     *
     * @return 当前数据类型内容对应的字节数组，如果当前数据类型为 Bulk Strings，有可能返回 {@code null}
     * @throws UnsupportedOperationException 如果当前数据类型是 Arrays 类型数据，将会抛出此异常
     */
    public byte[] getValueBytes() throws UnsupportedOperationException {
        throw new UnsupportedOperationException("Arrays has no value bytes.");
    }

    /**
     * 返回 Arrays 类型数据内容的数组长度，如果 Arrays 为 {@code null}，该方法将返回 -1。
     * <p>非 Arrays 类型数据不允许调用此方法，如果调用，将会抛出 {@link UnsupportedOperationException} 异常。</p>
     *
     * @throws UnsupportedOperationException 如果当前数据类型是非 Arrays 类型数据，将会抛出此异常
     * @see #isArray()
     * @see #get(int)
     */
    public int size() throws UnsupportedOperationException {
        throw new UnsupportedOperationException(getClass() + " is not arrays.");
    }

    /**
     * 获得 Arrays 类型数据指定索引位置的数据
     * <p>非 Arrays 类型数据不允许调用此方法，如果调用，将会抛出 {@link UnsupportedOperationException} 异常。</p>
     *
     * @param index 索引位置
     * @return 在 Arrays 类型数据指定索引位置对应的数据
     * @throws UnsupportedOperationException 如果当前数据类型是非 Arrays 类型数据，将会抛出此异常
     * @throws IndexOutOfBoundsException 如果索引越界或为负数，将抛出此异常
     */
    public RedisData get(int index) throws UnsupportedOperationException, IndexOutOfBoundsException {
        throw new UnsupportedOperationException(getClass() + " is not arrays.");
    }

    /**
     * 将当前数据类型内容对应的字节数组编码成字符串后返回，如果当前数据类型为 Bulk Strings，有可能返回 {@code null}。
     * <p>Arrays 类型数据不允许调用此方法，如果调用，将会抛出 {@link UnsupportedOperationException} 异常。</p>
     *
     * @return 当前数据类型内容对应的字节数组编码成字符串后返回，如果当前数据类型为 Bulk Strings，有可能返回 {@code null}
     * @throws UnsupportedOperationException 如果当前数据类型是 Arrays 类型数据，将会抛出此异常
     */
    public String getText() throws UnsupportedOperationException {
        byte[] valueBytes = getValueBytes();
        if (valueBytes != null) {
            return new String(valueBytes, UTF8);
        } else {
            return null;
        }
    }

    /**
     * 获得当前数据使用 RESP(Redis Serialization Protocol) 协议序列化后的字节数组
     *
     * @return 当前数据使用 RESP(Redis Serialization Protocol) 协议序列化后的字节数组
     */
    public abstract byte[] getRespByteArray();

}
