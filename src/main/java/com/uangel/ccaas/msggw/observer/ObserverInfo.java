package com.uangel.ccaas.msggw.observer;

import com.uangel.ccaas.aibotmsg.Message;
import io.grpc.stub.StreamObserver;
import lombok.Getter;

@Getter
public class ObserverInfo {

    private final String tId;
    private final StreamObserver<Message> responseObserver;
    private final long startTime;

    public ObserverInfo(String tId, StreamObserver<Message> responseObserver) {
        this.tId = tId;
        this.responseObserver = responseObserver;
        this.startTime = System.currentTimeMillis();
    }
}
