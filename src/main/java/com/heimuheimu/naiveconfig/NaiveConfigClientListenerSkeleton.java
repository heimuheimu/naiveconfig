package com.heimuheimu.naiveconfig;

/**
 * NaiveConfig 客户端事件监听器抽象实现类，继承该类的监听器，仅需重载自己所关心的事件，
 * 可防止 {@link NaiveConfigClientListener} 在后续版本增加方法时，需重新调整监听器实现类。
 *
 * @author heimuheimu
 */
public abstract class NaiveConfigClientListenerSkeleton implements NaiveConfigClientListener {

    @Override
    public <T> void onChanged(NaiveConfigClient client, String key, T newValue) {
        //do nothing
    }

    @Override
    public void onClosed(NaiveConfigClient client) {
        //do nothing
    }

    @Override
    public void onRecovered(NaiveConfigClient client) {
        //do nothing
    }
}
