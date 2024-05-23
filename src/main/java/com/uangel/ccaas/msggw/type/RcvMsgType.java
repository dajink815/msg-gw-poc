package com.uangel.ccaas.msggw.type;

import lombok.Getter;

@Getter
public enum RcvMsgType {
    RMQ(0), GRPC(1);

    private final int value;

    RcvMsgType(int value) {
        this.value = value;
    }

    public static RcvMsgType getType(int value) {
        if (value == 0) return RMQ;
        return GRPC;
    }
}
