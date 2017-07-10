package com.heimuheimu.naiveconfig;

import com.heimuheimu.naiveconfig.exception.NaiveConfigException;

/**
 * NaiveConfig 配置管理器，提供配置信息获取、设置、删除等管理操作
 * <p><b>注意：</b>该接口的实现类必须保证线程安全</p>
 *
 * @author heimuheimu
 * @ThreadSafe
 */
public interface NaiveConfigManager {

    /**
     * 配置信息 Key 最大字节数：250 B
     */
    int MAX_KEY_LENGTH = 250;

    /**
     * 获得配置中心远程主机地址，例如：localhost:6379
     * <p>注意：主机地址格式由实现类自行定义，没有固定格式</p>
     *
     * @return 配置中心远程主机地址
     */
    @SuppressWarnings("unused")
    String getHost();

    /**
     * 获取 Key 对应的配置信息值，如果 Key 不存在，将返回 {@code null}
     *
     * @param key 配置信息 Key
     * @return 配置信息，如果 Key 不存在，将返回 {@code null}
     * @throws NullPointerException 如果 Key 为 {@code null}，将抛出此异常
     * @throws IllegalArgumentException Key 字节长度超过 {@link #MAX_KEY_LENGTH}，将抛出此异常
     * @throws NaiveConfigException 获取配置信息过程中如果发生异常，将抛出此异常
     */
    @SuppressWarnings("unused")
    <T> T get(String key) throws NullPointerException, IllegalArgumentException, NaiveConfigException;

    /**
     * 在配置中心设置 Key 对应的配置信息，并通知已监听该配置信息变更的 NaiveConfig 客户端，返回成功接收该变更信息的 NaiveConfig 客户端数量。
     * <p>如果具体实现无法返回成功接收变更的客户端数量，应返回 -1</p>
     *
     * @param key 配置信息 Key，不允许为 {@code null}
     * @param value 配置信息，不允许为 {@code null}
     * @return receivedClients 成功接收该变更信息的 NaiveConfig 客户端数量
     * @throws NullPointerException 如果 Key 为 {@code null}，将抛出此异常
     * @throws NullPointerException 如果 Value 为 {@code null}，将抛出此异常
     * @throws IllegalArgumentException Key 字节长度超过 {@link #MAX_KEY_LENGTH}，将抛出此异常
     * @throws NaiveConfigException 设置配置信息过程中如果发生异常，将抛出此异常
     */
    @SuppressWarnings("unused")
    int set(String key, Object value) throws NullPointerException, IllegalArgumentException, NaiveConfigException;

    /**
     * 在配置中心删除 Key 对应的配置信息，并通知已监听该配置信息变更的 NaiveConfig 客户端，返回成功接收该变更信息的 NaiveConfig 客户端数量。
     * <p>如果具体实现无法返回成功接收变更的客户端数量，应返回 -1</p>
     *
     * @param key 配置信息 Key，不允许为 {@code null}
     * @return receivedClients 成功接收该变更信息的 NaiveConfig 客户端数量
     * @throws NullPointerException 如果 Key 为 {@code null}，将抛出此异常
     * @throws IllegalArgumentException Key 字节长度超过 {@link NaiveConfigManager#MAX_KEY_LENGTH}，将抛出此异常
     * @throws NaiveConfigException 删除配置信息过程中如果发生异常，将抛出此异常
     */
    @SuppressWarnings("unused")
    int delete(String key) throws NullPointerException, IllegalArgumentException, NaiveConfigException;

}
