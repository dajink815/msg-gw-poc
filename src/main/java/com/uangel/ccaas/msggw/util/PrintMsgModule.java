package com.uangel.ccaas.msggw.util;

import com.google.protobuf.InvalidProtocolBufferException;
import com.uangel.ccaas.aibotmsg.Message;
import com.uangel.ccaas.msggw.message.util.MessageParser;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PrintMsgModule extends MessageParser {
    private static final Suppress suppress = new Suppress(1000L * 30);
    private static final String PRINT_SEND_LOG = "[MSG] sendTo --> [{}] {}";
    private static final String PRINT_RCV_LOG = "[MSG] onReceived: {}";
    private static final String PRINT_RCV_MSG_TYPE = "[MSG] msgFrom <-- [{}] {}";

    private PrintMsgModule() {
        super();
    }

    public static void printSendLog(String target, Message message) throws InvalidProtocolBufferException {
        if (message == null) {
            return;
        }
        String jsonMsg = JsonUtil.printMessage(message);
        String msgType = getMsgType(message);
        log.info(PRINT_SEND_LOG, target, msgType);
        log.debug(PRINT_SEND_LOG, target, jsonMsg);
    }

    public static void printRcvLog(Message message) throws InvalidProtocolBufferException {
        String jsonMsg = JsonUtil.printMessage(message);
        String msgFrom = getMsgFrom(message);
        String msgType = getMsgType(message);
        log.info(PRINT_RCV_MSG_TYPE, msgFrom, msgType);
        log.debug(PRINT_RCV_LOG, jsonMsg);
    }


}
