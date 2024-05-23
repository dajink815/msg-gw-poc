package com.uangel.ccaas.msggw.message.handler;

import com.uangel.ccaas.aibotmsg.Message;
import com.uangel.ccaas.msggw.message.aiwf.BotStartReq;
import com.uangel.ccaas.msggw.message.aiwf.BotStopReq;
import com.uangel.ccaas.msggw.message.aiwf.BotTalkReq;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;

import static com.uangel.ccaas.aibotmsg.Message.*;

@Slf4j
public class IncomingHandler {
    private static IncomingHandler incomingHandler;

    private IncomingHandler() {
    }

    public static IncomingHandler getInstance() {
        if (incomingHandler == null) incomingHandler = new IncomingHandler();
        return incomingHandler;
    }

    public void handle(Message request, int rcvType, StreamObserver<Message> responseObserver) {
        switch (request.getBodyCase().getNumber()) {
            case STARTREQ_FIELD_NUMBER:
                new BotStartReq().handle(request, rcvType, null);
                break;
            case TALKREQ_FIELD_NUMBER:
                new BotTalkReq().handle(request);
                break;
            case STOPREQ_FIELD_NUMBER:
                new BotStopReq().handle(request);
                break;
            default:
                log.warn("GwRmqConsumer - Message type mismatch\n" + "[{}]", request.getHeader().getType());
        }
    }

}
