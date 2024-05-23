package com.uangel.ccaas.msggw.session;

import com.uangel.ccaas.msggw.type.RcvMsgType;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class SessionManager {
    private static SessionManager sessionManager = null;
    @Getter
    private final ConcurrentHashMap<String, SessionInfo> sessionMaps;

    private SessionManager() {
        sessionMaps = new ConcurrentHashMap<>();
    }

    public static SessionManager getInstance() {
        if (sessionManager == null) sessionManager = new SessionManager();
        return sessionManager;
    }

    // SessionInfo 생성
    public SessionInfo createSessionInfo(String callId, RcvMsgType rcvType) {
        if (callId == null) return null;
        if (sessionMaps.containsKey(callId)) {
            log.warn("SessionInfo [{}] already exist.", callId);
            return null;
        }
        return sessionMaps.computeIfAbsent(callId, sessionInfo -> {
            SessionInfo newInfo = new SessionInfo(callId, rcvType);
            log.info("SessionInfo [{}] Create", callId);
            return newInfo;
        });
    }

    // SessionInfo 조회
    public SessionInfo getSessionInfo(String callId) {
        if (callId == null) return null;
        return sessionMaps.get(callId);
    }

    // SessionInfo 리스트 조회
    public Set<SessionInfo> getSessionInfos() {
        return new HashSet<>(sessionMaps.values());
    }

    // SessionInfo 삭제
    public SessionInfo deleteSessionInfo(String callId) {
        if (callId == null) return null;
        SessionInfo removedInfo = sessionMaps.remove(callId);
        if (removedInfo != null) {
            log.info("SessionInfo [{}] Removed", callId);
        }
        return removedInfo;
    }

}
