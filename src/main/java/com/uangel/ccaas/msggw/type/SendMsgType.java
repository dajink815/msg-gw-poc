package com.uangel.ccaas.msggw.type;

import lombok.Getter;

@Getter
public enum SendMsgType {
    HTTP(0), GRPC(1);

    private final int value;

    SendMsgType(int value) {
        this.value = value;
    }

    public static SendMsgType getType(int value) {
        if (value == 0) return HTTP;
        return GRPC;
    }
}
