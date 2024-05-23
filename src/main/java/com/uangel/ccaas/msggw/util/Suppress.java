package com.uangel.ccaas.msggw.util;

import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class Suppress {
    private final long interval;    // unit : ms
    private final Map<String, Long> lastTimeMap = new ConcurrentHashMap<>();

    public Suppress(long interval) {
        this.interval = interval;
    }

    public boolean touch(String key) {
        if (key == null) return false;
        long now = System.currentTimeMillis();
        long last = lastTimeMap.computeIfAbsent(key, k -> now);
        // 최초 1회는 바로 출력
        boolean accept = now - last >= interval || now == last;
        if (accept) {
            last = now;
            lastTimeMap.put(key, last);
        }
        return accept;
    }
}
