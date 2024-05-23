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

    public abstract void reply(Message response, StreamObserver<Message> responseObserver);
}
