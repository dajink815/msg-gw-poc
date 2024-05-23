package com.uangel.ccaas.msggw.message.util;

import com.uangel.ccaas.aibotmsg.Header;
import com.uangel.ccaas.aibotmsg.Message;

import java.util.Optional;

public class MessageParser {
    protected MessageParser() {
    }

    public static String getMsgType(Message message) {
        return Optional.of(message.getHeader())
                .map(Header::getType)
                .orElse("");
    }

    public static String getMsgFrom(Message message) {
        return Optional.of(message.getHeader())
                .map(Header::getMsgFrom)
                .orElse("");
    }

    public static String getTid(Message message) {
        return Optional.of(message.getHeader())
                .map(Header::getTId)
                .orElse("");
    }
}
