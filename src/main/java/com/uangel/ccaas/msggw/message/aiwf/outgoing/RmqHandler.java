package com.uangel.ccaas.msggw.message.aiwf.outgoing;

import com.google.protobuf.InvalidProtocolBufferException;
import com.uangel.ccaas.aibotmsg.Message;
import com.uangel.ccaas.msggw.util.PrintMsgModule;
import io.grpc.stub.StreamObserver;

public class RmqHandler extends OutgoingHandler {

    @Override
    public void reply(Message response, StreamObserver<Message> responseObserver) {

        try {
            if (rmqModule == null || !rmqModule.isConnected()) {

                return;
            }


            if (response != null) {
                String target = config.getAiwfQueue();
                rmqModule.sendMessage(target, response.toByteArray());
                PrintMsgModule.printSendLog(target, response);
            }
        } catch (InvalidProtocolBufferException e) {
            throw new RuntimeException(e);
        }

    }
}
