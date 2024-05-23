package com.uangel.ccaas.msggw.message.aiwf.outgoing;

import com.uangel.ccaas.aibotmsg.Message;
import com.uangel.ccaas.msggw.type.RcvMsgType;
import io.grpc.stub.StreamObserver;


public class AiwfSender {

    private static AiwfSender aiwfSender;

    private AiwfSender() {
    }

    public static AiwfSender getInstance() {
        if (aiwfSender == null) aiwfSender = new AiwfSender();
        return aiwfSender;
    }

    public void replyMsg(Message response, RcvMsgType rcvType, StreamObserver<Message> responseObserver) {
        OutgoingHandler outgoingHandler;
        if (RcvMsgType.RMQ.equals(rcvType)) {
            outgoingHandler = new RmqHandler();
        } else {
            outgoingHandler = new GrpcHandler();
        }

        outgoingHandler.reply(response, responseObserver);
    }
}
