package com.uangel.ccaas.msggw.message.aiwf;

import com.uangel.ccaas.aibotmsg.Message;
import com.uangel.ccaas.msggw.message.AiwfCallBack;
import com.uangel.ccaas.msggw.message.IncomingMessage;
import com.uangel.ccaas.msggw.message.ai.AiMsgSender;
import com.uangel.ccaas.msggw.session.SessionInfo;
import com.uangel.ccaas.msggw.type.RcvMsgType;

public class BotTalkReq extends IncomingMessage {
    private final int muteType = 0;

    public BotTalkReq() {
    }

    public void handle(Message request, RcvMsgType rcvType, AiwfCallBack callBack) {

        com.uangel.ccaas.aibotmsg.BotTalkReq botTalkReq = request.getTalkReq();
        // check SessionInfo Null

        String sessionId = botTalkReq.getSessionId();
        SessionInfo sessionInfo = findInfo(sessionId);

        int type = botTalkReq.getType();
        String question = botTalkReq.getQuestion();
        if (type == muteType || question.isEmpty()) {

            // 종료 멘트 전달 - HANGUP

            return;
        }

        sessionInfo.setTalkTid(request.getHeader().getTId());

        // AI 에 질의
        AiMsgSender.getInstance().sendAiRequest(sessionInfo, question, callBack);
    }

}
