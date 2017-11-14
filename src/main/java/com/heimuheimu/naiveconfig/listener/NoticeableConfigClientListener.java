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

package com.heimuheimu.naiveconfig.listener;

import com.heimuheimu.naiveconfig.NaiveConfigClient;
import com.heimuheimu.naiveconfig.NaiveConfigClientListenerSkeleton;
import com.heimuheimu.naivemonitor.MonitorUtil;
import com.heimuheimu.naivemonitor.alarm.NaiveServiceAlarm;
import com.heimuheimu.naivemonitor.alarm.ServiceAlarmMessageNotifier;
import com.heimuheimu.naivemonitor.alarm.ServiceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * 该监听器可用于 NaiveConfig 客户端在发生 NaiveConfig 服务不可用或者从不可用状态恢复时，进行实时通知。
 * 并通过 {@link ConfigSyncHandler} 进行变更配置同步，每个 {@link ConfigSyncHandler} 对应一个单独的配置 Key。
 *
 * @author heimuheimu
 */
public class NoticeableConfigClientListener extends NaiveConfigClientListenerSkeleton {

    private static final Logger LOG = LoggerFactory.getLogger(NoticeableConfigClientListener.class);

    /**
     * 调用 NaiveConfig 服务的项目名称
     */
    private final String project;

    /**
     * 调用 NaiveConfig 服务的主机名称
     */
    private final String host;

    /**
     * 服务不可用报警器
     */
    private final NaiveServiceAlarm naiveServiceAlarm;

    /**
     * 配置信息同步处理器列表
     */
    private final List<ConfigSyncHandler> handlerList;

    public NoticeableConfigClientListener(List<ConfigSyncHandler> handlerList, String project, List<ServiceAlarmMessageNotifier> notifierList) {
        this(handlerList, project, notifierList, null);
    }

    public NoticeableConfigClientListener(List<ConfigSyncHandler> handlerList, String project, List<ServiceAlarmMessageNotifier> notifierList,
                                          Map<String, String> hostAliasMap) {
        this.handlerList = handlerList;
        this.project = project;
        this.naiveServiceAlarm = new NaiveServiceAlarm(notifierList);
        String host = MonitorUtil.getLocalHostName();
        if (hostAliasMap != null && hostAliasMap.containsKey(host)) {
            this.host = hostAliasMap.get(host);
        } else {
            this.host = host;
        }

    }

    @Override
    @SuppressWarnings("unchecked")
    public void onInitialized(NaiveConfigClient client) {
        for (ConfigSyncHandler handler : handlerList) {
            syncConfig(client, handler);
        }
    }

    @Override
    public void onChanged(NaiveConfigClient client, String key) {
        boolean isValidKey = false;
        for (ConfigSyncHandler handler : handlerList) {
            if (handler.getKey().equals(key)) {
                isValidKey = true;
                syncConfig(client, handler);
                break;
            }
        }
        if (!isValidKey) {
            LOG.error("Could not find ConfigSyncHandler for key: `" + key + "`.");
        }
    }

    @Override
    public void onClosed(NaiveConfigClient client) {
        naiveServiceAlarm.onCrashed(getServiceContext(client));
    }

    @Override
    public void onRecovered(NaiveConfigClient client) {
        naiveServiceAlarm.onRecovered(getServiceContext(client));
        for (ConfigSyncHandler handler : handlerList) {
            syncConfig(client, handler);
        }
    }

    @SuppressWarnings("unchecked")
    private void syncConfig(NaiveConfigClient client, ConfigSyncHandler handler) {
        try {
            long startTime = System.currentTimeMillis();
            Object value = client.get(handler.getKey());
            handler.sync(value);
            LOG.info("Sync config success. Cost: `{}ms`. Key: `{}`. Config: `{}`.",
                    (System.currentTimeMillis() - startTime), handler.getKey(), value);
        } catch (Exception e) {
            LOG.error("Sync config failed: `" + e.getMessage() + "`. Key: `" + handler.getKey() + "`. Handler: `"
                    + handler + "`.", e);
        }
    }

    /**
     * 根据 NaiveConfig 客户端构造一个服务及服务所在的运行环境信息
     *
     * @param client NaiveConfig 客户端
     * @return 服务及服务所在的运行环境信息
     */
    protected ServiceContext getServiceContext(NaiveConfigClient client) {
        ServiceContext serviceContext = new ServiceContext();
        serviceContext.setName("NaiveConfig");
        serviceContext.setProject(project);
        serviceContext.setRemoteHost(client.getHost());
        return serviceContext;
    }
}
