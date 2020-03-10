/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2018 heimuheimu
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

package com.heimuheimu.naiveconfig.spring;

import com.heimuheimu.naiveconfig.redis.OneTimeRedisClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionVisitor;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.util.StringValueResolver;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 通过 Redis 远程加载 Spring 启动所需的配置信息，例如 DB 地址、缓存地址等，{@code PropertyRedisConfigurer} 将会对 "({" 开头，
 * 并以 "})" 结尾的变量进行匹配替换。
 *
 * <p>使用场景：相同的 DB 或缓存等地址配置在多个项目中被使用。</p>
 *
 * <p><b>注意：配置信息必须使用 {@link OneTimeRedisClient} 提前写入，类型为 {@link String}</b></p>
 *
 * <h3>Spring 配置示例：</h3>
 * <blockquote>
 * <pre>
 * {@code <!-- Redis 配置读取器-->
 *   <bean name="propertyRedisConfigurer" class="com.heimuheimu.naiveconfig.spring.PropertyRedisConfigurer">
 *     <constructor-arg index="0" value="127.0.0.1:6379" />
 *   </bean>
 *
 *   <!-- "({demo.memcache.host})" 变量远程加载配置 Demo -->
 *   <bean id="autoReconnectMemcachedClient" class="com.heimuheimu.naivecache.memcached.advance.AutoReconnectMemcachedClient" destroy-method="close">
 *     <constructor-arg index="0" value="({demo.memcache.host})" /> <!-- Memcached 服务地址 -->
 *   </bean>
 * }
 * </pre>
 * </blockquote>
 *
 * @author heimuheimu
 */
public class PropertyRedisConfigurer implements BeanFactoryPostProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(PropertyRedisConfigurer.class);

    /**
     * 一次性 Redis 客户端
     */
    private final OneTimeRedisClient configRedisClient;

    /**
     * 如果为 true，遇到无法识别的变量将会抛出 IllegalArgumentException 异常，如果为 false，将会忽略无法识别的变量
     */
    private final boolean strictlyMode;

    /**
     * Redis 配置变量名称正则表达式，以 "({" 开头，并以 "})" 结尾
     */
    private final Pattern REDIS_PROPERTY_PATTERN = Pattern.compile("\\(\\{(.+?)}\\)");

    /**
     * 构造一个 {@code PropertyRedisConfigurer} 实例，strictlyMode 默认为 {@code true}，当遇到无法识别的变量将会抛出 IllegalArgumentException 异常。
     *
     * @param configRedisHost Redis 服务主机地址，由主机名和端口组成，":"符号分割，例如：localhost:6379
     * @throws IllegalArgumentException 如果 Redis 服务主机地址不符合规则，将会抛出此异常
     */
    public PropertyRedisConfigurer(String configRedisHost) throws IllegalArgumentException {
        this(configRedisHost, true);
    }

    /**
     * 构造一个 PropertyRedisConfigurer 实例。
     *
     * @param configRedisHost Redis 服务主机地址，由主机名和端口组成，":"符号分割，例如：localhost:6379
     * @param strictlyMode 如果为 true，遇到无法识别的变量将会抛出 IllegalArgumentException 异常，如果为 false，将会忽略无法识别的变量
     * @throws IllegalArgumentException 如果 Redis 服务主机地址不符合规则，将会抛出此异常
     */
    public PropertyRedisConfigurer(String configRedisHost, boolean strictlyMode) throws IllegalArgumentException {
        this.configRedisClient = new OneTimeRedisClient(configRedisHost);
        this.strictlyMode = strictlyMode;
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactoryToProcess) throws BeansException {
        StringValueResolver valueResolver = new RedisStringValueResolver();
        BeanDefinitionVisitor visitor = new BeanDefinitionVisitor(valueResolver);

        String[] beanNames = beanFactoryToProcess.getBeanDefinitionNames();
        for (String curName : beanNames) {
            BeanDefinition bd = beanFactoryToProcess.getBeanDefinition(curName);
            try {
                visitor.visitBeanDefinition(bd);
            }
            catch (Exception ex) {
                throw new BeanDefinitionStoreException(bd.getResourceDescription(), curName, ex.getMessage());
            }
        }

        // New in Spring 2.5: resolve placeholders in alias target names and aliases as well.
        beanFactoryToProcess.resolveAliases(valueResolver);

        // New in Spring 3.0: resolve placeholders in embedded values such as annotation attributes.
        beanFactoryToProcess.addEmbeddedValueResolver(valueResolver);
    }

    private class RedisStringValueResolver implements StringValueResolver {

        @Override
        public String resolveStringValue(String strVal) {
            if (strVal == null || strVal.isEmpty() || !strVal.contains("({")) {
                return strVal;
            }
            Matcher matcher = REDIS_PROPERTY_PATTERN.matcher(strVal);
            StringBuffer buffer = new StringBuffer();
            while (matcher.find()) {
                String key = matcher.group(1);
                String value = configRedisClient.get(key);
                if (value != null) {
                    LOGGER.info("Read redis config success. `key`:`{}`. `value`:`{}`.", key, value);
                } else {
                    LOGGER.error("Could not find redis config property: `" + key + "`.");
                    if (strictlyMode) {
                        throw new IllegalArgumentException("Could not find redis config property: `" + key + "`.");
                    } else {
                        value = "({" + key + ")}";
                    }
                }
                matcher.appendReplacement(buffer, value);
            }
            matcher.appendTail(buffer);
            return buffer.toString();
        }
    }
}
