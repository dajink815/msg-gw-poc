package com.uangel.ccaas.msggw.message.handler;

import com.uangel.ccaas.aibotmsg.Message;
import com.uangel.ccaas.msggw.config.MsgGwConfig;
import com.uangel.ccaas.msggw.rmq.GwRmqManager;
import com.uangel.ccaas.msggw.service.AppInstance;
import io.grpc.stub.StreamObserver;
import umedia.rmq.RmqModule;

public abstract class OutgoingHandler {
    protected final RmqModule rmqModule = GwRmqManager.getInstance().getRmqModule();
    protected final MsgGwConfig config = AppInstance.getInstance().getConfig();

/*    public void handle(Message request, StreamObserver<Message> responseObserver) {
        // BotTalkReq 만 처리
        int bodyNum = request.getBodyCase().getNumber();
        if (bodyNum != TALKREQ_FIELD_NUMBER) {

            return;
        }

        BotTalkReq botTalkReq = request.getTalkReq();

        String question = botTalkReq.getQuestion();





    }*/


    public abstract void reply(Message response, StreamObserver<Message> responseObserver);
}
