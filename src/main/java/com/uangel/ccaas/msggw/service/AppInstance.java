package com.uangel.ccaas.msggw.service;

import com.uangel.ccaas.msggw.config.MsgGwConfig;
import lombok.Data;

import java.nio.file.Path;

@Data
public class AppInstance {
    private MsgGwConfig config = new MsgGwConfig();
    private Path configPath;

    private boolean rmqConnect;

    private AppInstance() {
        // nothing
    }

    // Inner Static Helper Class
    private static class SingletonHelper {
        private static final AppInstance SINGLETON = new AppInstance();
    }

    public static AppInstance getInstance() {
        // 호출하는 곳이 많은 singleton 클래스는 multi-thread 환경에서 안전하지 않음
        return SingletonHelper.SINGLETON;
    }

    public boolean isActive() {
        return true;
    }

}
