package com.heimuheimu.naiveconfig.redis.data;

/**
 * Simple strings 类型数据，该数据类型的第一个字节为 "+"。
 * RESP  (Redis Serialization Protocol) 格式定义的更多信息请参考文档：<a href="https://redis.io/topics/protocol">https://redis.io/topics/protocol</a>
 *
 * @author heimuheimu
 */
public class RedisSimpleString extends RedisData {

    /**
     * Simple strings 类型数据第一个字节
     */
    public static final byte FIRST_BYTE = '+';

    /**
     * Simple strings 数据类型内容对应的字节数组，不包含第一个数据类型字节以及结尾 CR、LF 符，不允许 {@code null}
     */
    private final byte[] valueBytes;

    /**
     * 构造一个 Simple strings 类型数据
     *
     * @param valueBytes 数据类型内容对应的字节数组，不包含第一个数据类型字节以及结尾 CR、LF 符，不允许 {@code null}
     * @throws NullPointerException 如果传入的字节数组为 {@code null}，则抛出此异常
     */
    public RedisSimpleString(byte[] valueBytes) throws NullPointerException {
        if (valueBytes == null) {
            throw new NullPointerException("Redis simple string value bytes could not be null.");
        }
        this.valueBytes = valueBytes;
    }

    @Override
    public boolean isSimpleString() {
        return true;
    }

    @Override
    public byte[] getValueBytes() {
        return valueBytes;
    }

    @Override
    public byte[] getRespByteArray() {
        byte[] packet = new byte[3 + valueBytes.length];
        packet[0] = FIRST_BYTE;
        System.arraycopy(valueBytes, 0, packet, 1, valueBytes.length);
        packet[packet.length - 2] = CR;
        packet[packet.length - 1] = LF;
        return packet;
    }

    @Override
    public String toString() {
        return "RedisSimpleString{" +
                "value=" + getText() +
                '}';
    }

}
