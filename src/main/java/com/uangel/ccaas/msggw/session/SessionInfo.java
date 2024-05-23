package com.uangel.ccaas.msggw.session;

import lombok.Data;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Data
public class SessionInfo {

    @NonNull
    private final String callId;
    private final int rcvType;  // 0: RMQ, 1: gRPC
    private final long startTime;

    public SessionInfo(@NonNull String callId, int rcvType) {
        this.callId = callId;
        this.rcvType = rcvType;
        this.startTime = System.currentTimeMillis();
    }
}
