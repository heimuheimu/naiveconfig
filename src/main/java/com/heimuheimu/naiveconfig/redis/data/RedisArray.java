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

import java.util.Arrays;

/**
 * Arrays 类型数据，该数据类型的第一个字节为 "*"。
 * RESP  (Redis Serialization Protocol) 格式定义的更多信息请参考文档：<a href="https://redis.io/topics/protocol">https://redis.io/topics/protocol</a>
 *
 * @author heimuheimu
 */
public class RedisArray extends RedisData {

    private static final byte[] NULL_RESP_BYTE_ARRAY = "*-1\r\n".getBytes(UTF8);

    private static final byte[] EMPTY_RESP_BYTE_ARRAY = "*0\r\n".getBytes(UTF8);

    /**
     * Arrays 类型数据第一个字节
     */
    public static final byte FIRST_BYTE = '*';

    /**
     * Arrays 数据类型内容，允许为 {@code null}
     */
    private final RedisData[] value;

    /**
     * 构造一个 Arrays 类型数据
     *
     * @param value Arrays 数据类型内容，允许为 {@code null}
     */
    public RedisArray(RedisData[] value) {
        this.value = value;
    }

    @Override
    public boolean isArray() {
        return true;
    }

    @Override
    public int size() {
        return value != null ? value.length : -1;
    }

    @Override
    public RedisData get(int index) throws ArrayIndexOutOfBoundsException {
        return value[index];
    }

    @Override
    public byte[] getRespByteArray() {
        if (value == null) {
            return NULL_RESP_BYTE_ARRAY;
        } else if (value.length == 0) {
            return EMPTY_RESP_BYTE_ARRAY;
        } else {
            byte[][] dataByteArrays = new byte[value.length][];
            byte[] valueLengthBytes = String.valueOf(value.length).getBytes(UTF8);
            int totalByteSize = 0;
            for (int i = 0; i < value.length; i++) {
                dataByteArrays[i] = value[i].getRespByteArray();
                totalByteSize += dataByteArrays[i].length;
            }
            byte[] packet = new byte[3 + valueLengthBytes.length + totalByteSize];
            packet[0] = FIRST_BYTE;
            System.arraycopy(valueLengthBytes, 0, packet, 1, valueLengthBytes.length);
            packet[valueLengthBytes.length + 1] = CR;
            packet[valueLengthBytes.length + 2] = LF;
            int destPost = valueLengthBytes.length + 3;
            for (byte[] dataByteArray : dataByteArrays) {
                System.arraycopy(dataByteArray, 0, packet, destPost, dataByteArray.length);
                destPost += dataByteArray.length;
            }
            return packet;
        }
    }

    @Override
    public String toString() {
        return "RedisArray{" +
                "value=" + Arrays.toString(value) +
                '}';
    }

}
