package com.uangel.ccaas.msggw.message.aiwf;


import com.uangel.ccaas.aibotmsg.BotStartRes;
import com.uangel.ccaas.aibotmsg.Header;
import com.uangel.ccaas.aibotmsg.Message;
import com.uangel.ccaas.msggw.message.AiBotOutMessage;
import com.uangel.ccaas.msggw.message.IncomingMessage;
import com.uangel.ccaas.msggw.message.type.AiBotMsgType;
import com.uangel.ccaas.msggw.session.SessionInfo;
import com.uangel.ccaas.msggw.type.ChatType;
import io.grpc.stub.StreamObserver;

public class BotStartReq extends IncomingMessage {

    public void handle(Message request, int rcvType, StreamObserver<Message> responseObserver) {

        com.uangel.ccaas.aibotmsg.BotStartReq botTalkReq = request.getStartReq();

        // check SessionInfo

        String sessionId = botTalkReq.getSessionId();
        SessionInfo sessionInfo = sessionManager.createSessionInfo(sessionId, rcvType);

        Header header = request.getHeader();
        String msgFrom = header.getMsgFrom();

        // build response
        BotStartRes botStartRes = BotStartRes.newBuilder()
                .setSessionId("AI_BOT_ID")
                .setType(ChatType.TALK.getValue())
                // TtsMessages
                .addAnswer(buildTtsMessage("테스트입니다 - Start"))
                .addAnswer(buildTtsMessage("두번째 테스트입니다 - Start"))
                .build();
        Message response = new AiBotOutMessage(AiBotMsgType.BOT_START_RES, header.getTId(), header.getBotId(), botStartRes, msgFrom).build();

        aiwfSender.replyMsg(sessionInfo, response, responseObserver);

    }
}
