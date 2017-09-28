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
 * Bulk strings 类型数据，该数据类型的第一个字节为 "$"。
 * RESP  (Redis Serialization Protocol) 格式定义的更多信息请参考文档：<a href="https://redis.io/topics/protocol">https://redis.io/topics/protocol</a>
 *
 * @author heimuheimu
 */
public class RedisBulkString extends RedisData {

    private static final byte[] NULL_RESP_BYTE_ARRAY = "$-1\r\n".getBytes(UTF8);

    private static final byte[] EMPTY_RESP_BYTE_ARRAY = "$0\r\n\r\n".getBytes(UTF8);

    /**
     * Bulk strings 类型数据第一个字节
     */
    public static final byte FIRST_BYTE = '$';

    /**
     * Bulk strings 数据类型内容对应的字节数组，不包含第一个数据类型字节以及结尾 CR、LF 符，允许为 {@code null}
     */
    private final byte[] valueBytes;

    /**
     * 构造一个 Bulk strings 类型数据
     *
     * @param valueBytes 内容对应的字节数组，不包含第一个数据类型字节以及结尾 CR、LF 符，允许为 {@code null}
     */
    public RedisBulkString(byte[] valueBytes) {
        this.valueBytes = valueBytes;
    }

    @Override
    public boolean isBulkString() {
        return true;
    }

    @Override
    public byte[] getValueBytes() {
        return valueBytes;
    }

    @Override
    public byte[] getRespByteArray() {
        if (valueBytes == null) {
            return NULL_RESP_BYTE_ARRAY;
        } else if (valueBytes.length == 0) {
            return EMPTY_RESP_BYTE_ARRAY;
        } else {
            byte[] valueLengthBytes = String.valueOf(valueBytes.length).getBytes(UTF8);
            byte[] packet = new byte[5 + valueLengthBytes.length + valueBytes.length];
            packet[0] = FIRST_BYTE;
            System.arraycopy(valueLengthBytes, 0, packet, 1, valueLengthBytes.length);
            packet[valueLengthBytes.length + 1] = CR;
            packet[valueLengthBytes.length + 2] = LF;
            System.arraycopy(valueBytes, 0, packet, valueLengthBytes.length + 3, valueBytes.length);
            packet[packet.length - 2] = CR;
            packet[packet.length - 1] = LF;
            return packet;
        }
    }

    @Override
    public String toString() {
        return "RedisBulkString{" +
                "value=" + getText() +
                '}';
    }

}
