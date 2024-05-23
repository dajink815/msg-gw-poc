package com.uangel.ccaas.msggw.message.handler;

import com.uangel.ccaas.aibotmsg.Message;
import com.uangel.ccaas.msggw.message.AiwfCallBack;
import com.uangel.ccaas.msggw.message.aiwf.BotStartReq;
import com.uangel.ccaas.msggw.message.aiwf.BotStopReq;
import com.uangel.ccaas.msggw.message.aiwf.BotTalkReq;
import com.uangel.ccaas.msggw.type.RcvMsgType;
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

    public void handle(Message request, RcvMsgType rcvType, AiwfCallBack callBack) {

        switch (request.getBodyCase().getNumber()) {
            case STARTREQ_FIELD_NUMBER:
                // 세션 생성 후 바로 BotStartRes 응답
                new BotStartReq().handle(request, rcvType, callBack);
                break;
            case TALKREQ_FIELD_NUMBER:
                new BotTalkReq().handle(request, rcvType, callBack);
                break;
            case STOPREQ_FIELD_NUMBER:
                // 세션 삭제 후 바로 BotStopRes 응답
                new BotStopReq().handle(request, rcvType, callBack);
                break;
            default:
                log.warn("IncomingHandler - Message type mismatch\n" + "[{}]", request.getHeader().getType());
        }
    }

}
