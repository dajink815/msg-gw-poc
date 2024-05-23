package com.uangel.ccaas.msggw.observer;

import com.uangel.ccaas.aibotmsg.Message;
import io.grpc.stub.StreamObserver;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class ObserverManager {
    private static ObserverManager manager = null;
    @Getter
    private final ConcurrentHashMap<String, ObserverInfo> infoMap;

    private ObserverManager() {
        infoMap = new ConcurrentHashMap<>();
    }

    public static ObserverManager getInstance() {
        if (manager == null) manager = new ObserverManager();
        return manager;
    }

    public void putObserverInfo(String tId, StreamObserver<Message> responseObserver) {
        if (tId == null) return;

        infoMap.computeIfAbsent(tId, observerInfo -> {
            ObserverInfo newInfo = new ObserverInfo(tId, responseObserver);
            log.info("ObserverInfo [{}] Create", tId);
            return newInfo;
        });
    }

    public ObserverInfo getObserverInfo(String tId) {
        if (tId == null) return null;
        return infoMap.get(tId);
    }

    public ObserverInfo deleteObserverInfo(String tId) {
        if (tId == null) return null;
        ObserverInfo removedInfo = infoMap.remove(tId);
        if (removedInfo != null) {
            log.info("ObserverInfo [{}] Removed", tId);
        }
        return removedInfo;
    }
}
