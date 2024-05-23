package com.uangel.ccaas.msggw.message.aiwf;

import com.uangel.ccaas.aibotmsg.Message;
import com.uangel.ccaas.msggw.message.handler.GrpcHandler;
import com.uangel.ccaas.msggw.message.handler.OutgoingHandler;
import com.uangel.ccaas.msggw.message.handler.RmqHandler;
import com.uangel.ccaas.msggw.session.SessionInfo;
import io.grpc.stub.StreamObserver;


public class AiwfSender {

    private static AiwfSender aiwfSender;

    private AiwfSender() {
    }

    public static AiwfSender getInstance() {
        if (aiwfSender == null) aiwfSender = new AiwfSender();
        return aiwfSender;
    }

    public void replyMsg(SessionInfo sessionInfo, Message response, StreamObserver<Message> responseObserver) {

        OutgoingHandler outgoingHandler;
        if (sessionInfo.getRcvType() == 0) {  // RMQ
            outgoingHandler = new RmqHandler();
        } else {
            outgoingHandler = new GrpcHandler();
        }

        outgoingHandler.reply(response, responseObserver);
    }
}
