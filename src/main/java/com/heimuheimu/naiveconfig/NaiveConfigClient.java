package com.heimuheimu.naiveconfig;

import com.heimuheimu.naiveconfig.exception.NaiveConfigException;

/**
 * NaiveConfig 客户端，用于从配置中心获取配置信息，并可以通过监听器监听配置中心服务是否正常、配置信息是否发生变更等事件
 * <p><b>注意：</b>该接口的实现类必须保证线程安全</p>
 *
 * @author heimuheimu
 * @see NaiveConfigClientListener
 * @ThreadSafe
 */
public interface NaiveConfigClient {

    /**
     * 获得配置中心远程主机地址，例如：localhost:6379
     * <p>注意：主机地址格式由实现类自行定义，没有固定格式</p>
     *
     * @return 配置中心远程主机地址
     */
    String getHost();

    /**
     * 获取 Key 对应的配置信息值，如果 Key 不存在，将返回 {@code null}
     *
     * @param key 配置信息 Key
     * @return 配置信息，如果 Key 不存在，将返回 {@code null}
     * @throws NullPointerException 如果 Key 为 {@code null}，将抛出此异常
     * @throws IllegalArgumentException Key 字节长度超过 {@link NaiveConfigManager#MAX_KEY_LENGTH}，将抛出此异常
     * @throws NaiveConfigException 获取配置信息过程中如果发生异常，将抛出此异常
     */
    <T> T get(String key) throws NullPointerException, IllegalArgumentException, NaiveConfigException;

}
