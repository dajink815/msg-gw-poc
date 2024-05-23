package com.uangel.ccaas.msggw.message.aiwf;

import com.uangel.ccaas.aibotmsg.BotStartRes;
import com.uangel.ccaas.aibotmsg.Header;
import com.uangel.ccaas.aibotmsg.Message;
import com.uangel.ccaas.msggw.message.AiwfOutMessage;
import com.uangel.ccaas.msggw.message.AiwfCallBack;
import com.uangel.ccaas.msggw.message.IncomingMessage;
import com.uangel.ccaas.msggw.message.type.AiBotMsgType;
import com.uangel.ccaas.msggw.session.SessionInfo;
import com.uangel.ccaas.msggw.type.ChatType;
import com.uangel.ccaas.msggw.type.RcvMsgType;

public class BotStartReq extends IncomingMessage {

    public BotStartReq() {
    }

    public void handle(Message request, RcvMsgType rcvType, AiwfCallBack callBack) {
        com.uangel.ccaas.aibotmsg.BotStartReq botTalkReq = request.getStartReq();

        String sessionId = botTalkReq.getSessionId();
        SessionInfo sessionInfo = sessionManager.createSessionInfo(sessionId, rcvType);

        // check SessionInfo
        if (sessionInfo == null) {
            // fail response

            return;
        }


        Header header = request.getHeader();
        String botId = header.getBotId();
        sessionInfo.setBotId(botId);

                // build response
        BotStartRes botStartRes = BotStartRes.newBuilder()
                .setSessionId(sessionId)
                .setType(ChatType.TALK.getValue())
                //.setCode()
                //.setReason()
                // TtsMessages
                .addAnswer(buildTtsMessage("Start 메시지"))
                .addAnswer(buildTtsMessage("두번째 Start 메시지"))
                .build();
        Message response = new AiwfOutMessage(AiBotMsgType.BOT_START_RES, header.getTId(), header.getBotId(), botStartRes).build();

        callBack.reply(response);
    }
}
