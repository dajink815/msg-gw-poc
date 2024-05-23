package com.uangel.ccaas.msggw.message;


import com.uangel.ccaas.aibotmsg.TtsMessage;
import com.uangel.ccaas.msggw.message.aiwf.AiwfSender;
import com.uangel.ccaas.msggw.session.SessionInfo;
import com.uangel.ccaas.msggw.session.SessionManager;

// todo Type 받아서 header 랑 파싱...
public class IncomingMessage {
    protected final SessionManager sessionManager = SessionManager.getInstance();
    protected final AiwfSender aiwfSender = AiwfSender.getInstance();

    protected IncomingMessage() {
    }

    protected SessionInfo findInfo(String sessionId) {
        return sessionManager.getSessionInfo(sessionId);
    }

    protected TtsMessage buildTtsMessage(String content) {
        return TtsMessage.newBuilder()
                .setText(content)
                .setBargeIn(false)
                .build();
    }
}
