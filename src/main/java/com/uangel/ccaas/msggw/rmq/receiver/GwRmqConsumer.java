package com.uangel.ccaas.msggw.rmq.receiver;

import com.google.protobuf.InvalidProtocolBufferException;
import com.uangel.ccaas.aibotmsg.Message;
import com.uangel.ccaas.msggw.message.handler.IncomingHandler;
import com.uangel.ccaas.msggw.rmq.GwRmqManager;
import com.uangel.ccaas.msggw.service.AppInstance;
import com.uangel.ccaas.msggw.util.PrintMsgModule;
import lombok.extern.slf4j.Slf4j;
import umedia.rmq.RmqModule;

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
            RmqModule rmqModule = GwRmqManager.getInstance().getRmqModule();
            if (rmqModule == null || !rmqModule.isConnected()) {

                return;
            }

            IncomingHandler.getInstance().handle(request, response -> {
                try {
                    if (response != null) {
                        String target = AppInstance.getInstance().getConfig().getAiwfQueue();
                        rmqModule.sendMessage(target, response.toByteArray());
                        PrintMsgModule.printSendLog(target, response);
                    }
                } catch (InvalidProtocolBufferException e) {
                    throw new RuntimeException(e);
                }
            });
        } catch (Exception e) {
            log.warn("Failed to GwRmqConsumer.handleRmqMessage", e);
        }
    }

}
