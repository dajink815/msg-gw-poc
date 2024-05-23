package com.uangel.ccaas.msggw.message.aiwf;

import com.uangel.ccaas.aibotmsg.Message;
import com.uangel.ccaas.msggw.message.IncomingMessage;
import com.uangel.ccaas.msggw.session.SessionInfo;

public class BotTalkReq extends IncomingMessage {
    private final int muteType = 0;

    public void handle(Message request) {

        com.uangel.ccaas.aibotmsg.BotTalkReq botTalkReq = request.getTalkReq();
        int type = botTalkReq.getType();
        if (type == muteType) {


            return;
        }

        // check SessionInfo Null

        String sessionId = botTalkReq.getSessionId();
        SessionInfo sessionInfo = findInfo(sessionId);




    }

}
