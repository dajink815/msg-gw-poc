package com.uangel.ccaas.msggw.grpc;

import com.uangel.ccaas.aibotmsg.Message;
import com.uangel.ccaas.msggw.message.AiwfCallBack;
import lombok.Data;

@Data
public class GrpcMsgInfo {

    private final Message request;
    private AiwfCallBack aiwfCallBack;

    public GrpcMsgInfo(Message request) {
        this.request = request;
    }
}
