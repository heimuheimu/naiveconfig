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

package com.heimuheimu.naiveconfig;

import com.heimuheimu.naiveconfig.exception.NaiveConfigException;

/**
 * NaiveConfig 客户端，用于从配置中心获取配置信息，并可以通过监听器监听配置中心服务是否正常、配置信息是否发生变更等事件。
 *
 * <p><strong>说明：</strong> {@code NaiveConfigClient} 的实现类必须是线程安全的。</p>
 *
 * @see NaiveConfigClientListener
 * @author heimuheimu
 */
public interface NaiveConfigClient {

    /**
     * 获得配置中心远程主机地址，例如：localhost:6379。
     *
     * <p><strong>说明：</strong>主机地址格式由实现类自行定义。</p>
     *
     * @return 配置中心远程主机地址
     */
    String getHost();

    /**
     * 获取 Key 对应的配置信息值，如果 Key 不存在，将返回 {@code null}。
     *
     * @param key 配置信息 Key
     * @param <T> 配置信息 Value 类型
     * @return 配置信息，如果 Key 不存在，将返回 {@code null}
     * @throws NullPointerException 如果 Key 为 {@code null}，将抛出此异常
     * @throws IllegalArgumentException Key 字节长度超过 {@link NaiveConfigManager#MAX_KEY_LENGTH}，将抛出此异常
     * @throws NaiveConfigException 获取配置信息过程中如果发生异常，将抛出此异常
     */
    <T> T get(String key) throws NullPointerException, IllegalArgumentException, NaiveConfigException;

}
