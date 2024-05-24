package com.uangel.ccaas.msggw.session;

import lombok.Data;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Data
public class SessionInfo {

    @NonNull
    private final String sessionId;
    private final long startTime;
    private String botId;
    private String talkTid;

    public SessionInfo(@NonNull String sessionId) {
        this.sessionId = sessionId;
        this.startTime = System.currentTimeMillis();
    }
}
