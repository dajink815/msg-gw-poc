package com.uangel.ccaas.msggw.session;

import com.uangel.ccaas.msggw.type.RcvMsgType;
import lombok.Data;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Data
public class SessionInfo {

    @NonNull
    private final String sessionId;
    private final RcvMsgType rcvType;  // 0: RMQ, 1: gRPC
    private final long startTime;
    private String botId;
    private String talkTid;

    public SessionInfo(@NonNull String sessionId, RcvMsgType rcvType) {
        this.sessionId = sessionId;
        this.rcvType = rcvType;
        this.startTime = System.currentTimeMillis();
    }
}
