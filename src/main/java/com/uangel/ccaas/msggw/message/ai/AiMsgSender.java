package com.uangel.ccaas.msggw.message.ai;

import com.uangel.ccaas.msggw.config.MsgGwConfig;
import com.uangel.ccaas.msggw.message.AiwfCallBack;
import com.uangel.ccaas.msggw.service.AppInstance;
import com.uangel.ccaas.msggw.session.SessionInfo;
import com.uangel.ccaas.msggw.type.SendMsgType;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AiMsgSender {
    private static AiMsgSender aiMsgSender;
    private final MsgGwConfig config = AppInstance.getInstance().getConfig();

    private AiMsgSender() {
    }

    public static AiMsgSender getInstance() {
        if (aiMsgSender == null) aiMsgSender = new AiMsgSender();
        return aiMsgSender;
    }

    public void sendAiRequest(SessionInfo sessionInfo, String question, AiwfCallBack callBack) {
        if (SendMsgType.HTTP.equals(config.getSendMsgType())) {
            // todo
        } else if (SendMsgType.GRPC.equals(config.getSendMsgType())) {
            AiGrpcSender sender = new AiGrpcSender();
            sender.sendAiRequest(sessionInfo, question, callBack);
            sender.shutdown();
        } else {
            log.warn("");
        }
    }

}
