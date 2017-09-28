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

import com.heimuheimu.naiveconfig.redis.data.*;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Redis 数据读取器，从字节流中读取 {@link com.heimuheimu.naiveconfig.redis.data.RedisData}
 * <p>注意：当前实现是非线程安全的</p>
 *
 * @author heimuheimu
 * @NotThreadSafe
 */
public class RedisDataReader {

    private final BufferedInputStream bis;

    public RedisDataReader(InputStream inputStream) {
        this.bis = new BufferedInputStream(inputStream);
    }

    public RedisData read() throws IOException {
        int firstByte = bis.read();
        switch (firstByte) {
            case RedisSimpleString.FIRST_BYTE:
                return readSimpleString();
            case RedisError.FIRST_BYTE:
                return readError();
            case RedisInteger.FIRST_BYTE:
                return readInteger();
            case RedisBulkString.FIRST_BYTE:
                return readBulkString();
            case RedisArray.FIRST_BYTE:
                return readArray();
            case -1: //end of the stream is reached
                return null;
            default:
                throw new IOException("Unknown first byte: `" + firstByte + "`.");
        }
    }

    private RedisSimpleString readSimpleString() throws IOException {
        byte[] valueBytes = readLine();
        if (valueBytes != null) {
            return new RedisSimpleString(valueBytes);
        } else {
            return null;
        }
    }

    private RedisError readError() throws IOException {
        byte[] valueBytes = readLine();
        if (valueBytes != null) {
            return new RedisError(valueBytes);
        } else {
            return null;
        }
    }

    private RedisInteger readInteger() throws IOException {
        byte[] valueBytes = readLine();
        if (valueBytes != null) {
            return new RedisInteger(valueBytes);
        } else {
            return null;
        }
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private RedisBulkString readBulkString() throws IOException {
        byte[] lengthBytes = readLine();
        if (lengthBytes != null) {
            int length = Integer.parseInt(new String(lengthBytes, RedisData.UTF8));
            if (length == -1) {
                return new RedisBulkString(null);
            } else {
                byte[] valueBytes = new byte[length];
                int valuePos = 0;
                while (valuePos < length) {
                    int readBytes = bis.read(valueBytes, valuePos, length - valuePos);
                    if (readBytes >= 0) {
                        valuePos += readBytes;
                    } else {
                        //end of the stream is reached
                        return null;
                    }
                }
                bis.read();
                bis.read();
                return new RedisBulkString(valueBytes);
            }
        } else {
            return null;
        }
    }

    private RedisArray readArray() throws IOException {
        byte[] lengthBytes = readLine();
        if (lengthBytes != null) {
            int length = Integer.parseInt(new String(lengthBytes, RedisData.UTF8));
            if (length == -1) {
                return new RedisArray(null);
            } else if (length == 0) {
                return new RedisArray(new RedisData[0]);
            } else {
                RedisData[] datas = new RedisData[length];
                for (int i = 0; i < length; i++) {
                    RedisData data = read();
                    if (data != null) {
                        datas[i] = data;
                    } else { //end of the stream is reached
                        return null;
                    }
                }
                return new RedisArray(datas);
            }
        } else {
            return null;
        }
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private byte[] readLine() throws IOException {
        bis.mark(Integer.MAX_VALUE);
        int length = 0;
        int readByte;
        while ((readByte = bis.read()) != RedisData.CR) {
            if (readByte == -1) { //end of the stream is reached
                return null;
            } else {
                length++;
            }
        }
        bis.reset();
        byte[] valueBytes = new byte[length];
        if (length > 0) {
            bis.read(valueBytes);
        }
        bis.read();
        bis.read();
        return valueBytes;
    }

}
