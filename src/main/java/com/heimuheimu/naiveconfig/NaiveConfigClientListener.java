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

/**
 * NaiveConfig 客户端事件监听器。
 *
 * <p>
 *     <strong>说明：</strong>监听器的实现类必须是线程安全的。应优先考虑继承 {@link NaiveConfigClientListenerSkeleton} 骨架类进行实现，
 *     防止 {@code NaiveMemcachedClientListener} 在后续版本增加方法时，带来的编译错误。
 * </p>
 *
 * @author heimuheimu
 */
public interface NaiveConfigClientListener {

    /**
     * NaiveConfig 客户端初始化完成后，将触发该监听事件，该事件仅触发一次。
     *
     * @param client 初始化完成的 NaiveConfig 客户端
     */
    void onInitialized(NaiveConfigClient client);

    /**
     * 当 Key 对应的配置信息发生变更时，将触发该监听事件。
     *
     * @param client 接收到配置信息变更事件的 NaiveConfig 客户端
     * @param key 配置信息发生变更的 Key
     */
    void onChanged(NaiveConfigClient client, String key);

    /**
     * NaiveConfig 客户端与配置中心远程主机数据交互通道不可用时，将触发该监听事件。
     *
     * @param client 发生数据交互通道关闭事件的 NaiveConfig 客户端
     */
    void onClosed(NaiveConfigClient client);

    /**
     * NaiveConfig 客户端与配置中心远程主机数据交互通道在关闭后重新恢复，将触发该监听事件。
     *
     * <p><strong>说明：</strong>为防止丢失通道关闭期间发生的配置信息变更事件，建议在当前事件中进行一次配置信息检查。</p>
     *
     * @param client 发生数据交互通道恢复事件的 NaiveConfig 客户端
     */
    void onRecovered(NaiveConfigClient client);

}
