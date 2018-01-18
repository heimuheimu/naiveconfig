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
 * NaiveConfig 配置管理器，提供配置信息获取、设置、删除等管理操作。
 *
 * <p><strong>说明：</strong> {@code NaiveConfigManager} 的实现类必须是线程安全的。</p>
 *
 * @author heimuheimu
 */
public interface NaiveConfigManager {

    /**
     * 配置信息 Key 最大字节数：250 B。
     */
    int MAX_KEY_LENGTH = 250;

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
     * @throws IllegalArgumentException Key 字节长度超过 {@link #MAX_KEY_LENGTH}，将抛出此异常
     * @throws NaiveConfigException 获取配置信息过程中如果发生异常，将抛出此异常
     */
    <T> T get(String key) throws NullPointerException, IllegalArgumentException, NaiveConfigException;

    /**
     * 在配置中心设置 Key 对应的配置信息，并通知已监听该配置信息变更的 NaiveConfig 客户端，返回成功接收该变更信息的 NaiveConfig 客户端数量。
     *
     * <p><strong>说明：</strong>如果具体实现无法返回成功接收变更的客户端数量，应返回 -1。</p>
     *
     * <p>Value 允许为 {@code null}，如果为 {@code null}，仅进行配置信息变更通知，不会在配置中心设置 Key 对应的配置信息。</p>
     *
     * @param key 配置信息 Key，不允许为 {@code null}
     * @param value 配置信息 Value，允许为 {@code null}，如果为 {@code null}，仅进行配置信息变更通知，不会在配置中心设置 Key 对应的配置信息
     * @return 成功接收该变更信息的 NaiveConfig 客户端数量
     * @throws NullPointerException 如果 Key 为 {@code null}，将抛出此异常
     * @throws IllegalArgumentException Key 字节长度超过 {@link #MAX_KEY_LENGTH}，将抛出此异常
     * @throws NaiveConfigException 设置配置信息过程中如果发生异常，将抛出此异常
     */
    int set(String key, Object value) throws NullPointerException, IllegalArgumentException, NaiveConfigException;
}
