package com.uangel.ccaas.msggw.rmq.receiver;

import com.uangel.ccaas.aibotmsg.Message;
import com.uangel.ccaas.msggw.message.handler.IncomingHandler;
import com.uangel.ccaas.msggw.type.RcvMsgType;
import com.uangel.ccaas.msggw.util.PrintMsgModule;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GwRmqConsumer extends umedia.rmq.RmqConsumer {

    public GwRmqConsumer(int consumerCount, int rmqBufferCount) {
        super(consumerCount, rmqBufferCount);
    }

    @Override
    public void handleRmqMessage(byte[] bytes) {
        try {
            Message request = Message.parseFrom(bytes);
            PrintMsgModule.printRcvLog(request);
            // handle
            IncomingHandler.getInstance().handle(request, RcvMsgType.RMQ, null);
        } catch (Exception e) {
            log.warn("Failed to GwRmqConsumer.handleRmqMessage", e);
        }
    }

}
