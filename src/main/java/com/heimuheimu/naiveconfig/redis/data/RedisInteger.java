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

package com.heimuheimu.naiveconfig.redis.data;

/**
 * Integers 类型数据，该数据类型的第一个字节为 ":"。
 * RESP  (Redis Serialization Protocol) 格式定义的更多信息请参考文档：<a href="https://redis.io/topics/protocol">https://redis.io/topics/protocol</a>
 *
 * @author heimuheimu
 */
public class RedisInteger extends RedisData {

    /**
     * Integers 类型数据第一个字节
     */
    public static final byte FIRST_BYTE = ':';

    /**
     * Integers 数据类型内容对应的字节数组，不包含第一个数据类型字节以及结尾 CR、LF 符，不允许 {@code null}
     */
    private final byte[] valueBytes;

    /**
     * 构造一个 Integers 类型数据。
     *
     * @param valueBytes 数据类型内容对应的字节数组，不包含第一个数据类型字节以及结尾 CR、LF 符，不允许 {@code null}
     * @throws NullPointerException 如果传入的字节数组为 {@code null}，则抛出此异常
     */
    public RedisInteger(byte[] valueBytes) throws NullPointerException {
        if (valueBytes == null) {
            throw new NullPointerException("Redis integers value bytes could not be null.");
        }
        this.valueBytes = valueBytes;
    }

    @Override
    public boolean isInteger() {
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
        return "RedisInteger{" +
                "value=" + getText() +
                '}';
    }

}
