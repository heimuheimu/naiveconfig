package com.heimuheimu.naiveconfig.exception;

/**
 * 从配置中心获取配置或者设置配置时遇到错误，将会抛出此异常
 *
 * @author heimuheimu
 */
public class NaiveConfigException extends RuntimeException {

    private static final long serialVersionUID = 6305753996453818689L;

    public NaiveConfigException(String message) {
        super(message);
    }

    public NaiveConfigException(String message, Throwable cause) {
        super(message, cause);
    }

}
