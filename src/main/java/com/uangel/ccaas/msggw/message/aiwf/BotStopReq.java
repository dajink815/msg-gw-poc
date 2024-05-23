package com.uangel.ccaas.msggw.message.aiwf;

import com.uangel.ccaas.aibotmsg.BotStopRes;
import com.uangel.ccaas.aibotmsg.Header;
import com.uangel.ccaas.aibotmsg.Message;
import com.uangel.ccaas.msggw.message.AiBotOutMessage;
import com.uangel.ccaas.msggw.message.AiwfCallBack;
import com.uangel.ccaas.msggw.message.IncomingMessage;
import com.uangel.ccaas.msggw.message.type.AiBotMsgType;
import com.uangel.ccaas.msggw.session.SessionInfo;
import com.uangel.ccaas.msggw.type.RcvMsgType;
import io.grpc.stub.StreamObserver;

public class BotStopReq extends IncomingMessage {

    public void handle(Message request, RcvMsgType rcvType,  AiwfCallBack callBack) {
        com.uangel.ccaas.aibotmsg.BotStopReq botStopReq = request.getStopReq();

        String sessionId = botStopReq.getSessionId();
        SessionInfo sessionInfo = sessionManager.deleteSessionInfo(sessionId);
        if (sessionInfo == null) {
            // fail response?
        }

        Header header = request.getHeader();
        BotStopRes botStopRes = BotStopRes.newBuilder()
                .setSessionId(botStopReq.getSessionId())
                .build();
        Message response = new AiBotOutMessage(AiBotMsgType.BOT_STOP_RES, header.getTId(), header.getBotId(),botStopRes).build();

        /*aiwfSender.replyMsg(response, rcvType, responseObserver);*/
        callBack.reply(response);

    }
}
