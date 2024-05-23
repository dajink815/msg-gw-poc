package com.uangel.ccaas.msggw.message;

import com.uangel.ccaas.aibotmsg.*;

import com.uangel.ccaas.msggw.message.type.AiBotMsgType;
import com.uangel.ccaas.msggw.service.AppInstance;
import com.uangel.ccaas.msggw.util.DateFormatUtil;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AiBotOutMessage {

    private final Header header;

    private final Message body;

    public AiBotOutMessage(AiBotMsgType type, String tId, String botId, Object o) {
        this(type, tId, botId, o, AppInstance.getInstance().getConfig().getMsgGwQueue());
    }

    public AiBotOutMessage(AiBotMsgType type, String tId, String botId, Object o, String msgFrom) {
        this.header = Header.newBuilder()
                .setType(type.name())
                .setTId(tId)
                .setMsgFrom(msgFrom)
                .setTimestamp(DateFormatUtil.currentTimeStamp())
                .setBotId(botId).build();
        this.body = buildBody(o);
    }

    private Message buildBody(Object o) {
        Message message = null;
        if (o instanceof BotStartReq bodyObj) {
            message = Message.newBuilder().setStartReq(bodyObj).build();
        } else if (o instanceof BotStartRes bodyObj) {
            message = Message.newBuilder().setStartRes(bodyObj).build();
        } else if (o instanceof BotTalkReq bodyObj) {
            message = Message.newBuilder().setTalkReq(bodyObj).build();
        } else if (o instanceof BotTalkRes bodyObj) {
            message = Message.newBuilder().setTalkRes(bodyObj).build();
        } else if (o instanceof BotStopReq bodyObj) {
            message = Message.newBuilder().setStopReq(bodyObj).build();
        } else if (o instanceof BotStopRes bodyObj) {
            message = Message.newBuilder().setStopRes(bodyObj).build();
        } else {
            log.warn("AiBotOutMessage.buildBody - Message type mismatch\n" + "[{}]", o);
        }

        return message;
    }

    public Message build() {
        if (this.body == null) return null;
        return this.body.toBuilder().setHeader(this.header).build();
    }
}
